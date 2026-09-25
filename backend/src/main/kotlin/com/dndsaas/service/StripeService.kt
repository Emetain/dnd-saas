package com.dndsaas.service

import com.dndsaas.config.StripeProperties
import com.dndsaas.domain.StripeEvent
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.TokenPack
import com.dndsaas.domain.User
import com.dndsaas.repository.StripeEventRepository
import com.dndsaas.repository.SubscriptionRepository
import com.dndsaas.repository.UserRepository
import com.stripe.Stripe
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.model.Price
import com.stripe.model.Subscription
import com.stripe.model.billingportal.Configuration
import com.stripe.net.Webhook
import com.stripe.param.CustomerCreateParams
import com.stripe.param.PriceCreateParams
import com.stripe.param.PriceListParams
import com.stripe.param.billingportal.ConfigurationCreateParams
import com.stripe.param.billingportal.ConfigurationListParams
import com.stripe.param.checkout.SessionCreateParams
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import com.stripe.model.billingportal.Session as PortalSession
import com.stripe.model.checkout.Session as CheckoutSession
import com.stripe.param.billingportal.SessionCreateParams as PortalSessionParams

/**
 * Payments through Stripe: Checkout for plans and token packs, the Customer
 * Portal for managing a subscription, and webhooks that turn payments into
 * tokens and tiers.
 *
 * Nothing needs to be set up in the Stripe dashboard by hand: the prices and
 * the portal configuration are created on first use, found again by their
 * lookup keys / metadata afterwards. Tokens are only ever granted from
 * verified webhooks, never from the browser.
 */
@Service
class StripeService(
    private val properties: StripeProperties,
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val stripeEventRepository: StripeEventRepository,
    private val subscriptionService: SubscriptionService,
    private val tokenService: TokenService,
    private val userService: UserService,
) {
    private val log = LoggerFactory.getLogger(StripeService::class.java)

    /** Prices by lookup key, cached after the first lookup. */
    private val prices = ConcurrentHashMap<String, Price>()

    @Volatile
    private var portalConfigurationId: String? = null

    @PostConstruct
    fun init() {
        if (properties.isConfigured) {
            Stripe.apiKey = properties.secretKey
            log.info("Stripe payments enabled ({} mode)", if (properties.isTestMode) "TEST" else "LIVE")
        } else {
            log.warn("STRIPE_SECRET_KEY is not set — payments are off and plan changes are free (test mode)")
        }
    }

    val paymentsEnabled: Boolean get() = properties.isConfigured

    /** Stripe test mode: test cards only, no real money. */
    val testMode: Boolean get() = properties.isTestMode

    // -----------------------------------------------------------------------
    // Checkout and portal
    // -----------------------------------------------------------------------

    /** Starts a Stripe Checkout for a monthly plan. Returns the URL to send the user to. */
    @Transactional
    fun checkoutForPlan(userId: Long, tier: SubscriptionTier): String {
        requireConfigured()
        require(tier != SubscriptionTier.FREE) { "Free does not need a checkout" }
        val user = userService.findEntity(userId)
        val active = subscriptionRepository.findByUserAndIsActiveTrue(user)
        if (active?.stripeSubscriptionId != null) {
            throw AlreadySubscribedException("You already have a subscription — change it with “Manage subscription”")
        }

        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(customerId(user))
            .setClientReferenceId(userId.toString())
            .addLineItem(SessionCreateParams.LineItem.builder().setPrice(planPrice(tier).id).setQuantity(1L).build())
            .setSubscriptionData(
                SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("userId", userId.toString())
                    .putMetadata("tier", tier.name)
                    .build(),
            )
            .putMetadata("userId", userId.toString())
            .setSuccessUrl("${properties.returnBaseUrl}/billing?checkout=success")
            .setCancelUrl("${properties.returnBaseUrl}/billing?checkout=cancelled")
            .build()
        return CheckoutSession.create(params).url
    }

    /** Starts a Stripe Checkout for a one-off token pack. */
    @Transactional
    fun checkoutForPack(userId: Long, pack: TokenPack): String {
        requireConfigured()
        val user = userService.findEntity(userId)
        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setCustomer(customerId(user))
            .setClientReferenceId(userId.toString())
            .addLineItem(SessionCreateParams.LineItem.builder().setPrice(packPrice(pack).id).setQuantity(1L).build())
            .putMetadata("userId", userId.toString())
            .putMetadata("pack", pack.name)
            .setSuccessUrl("${properties.returnBaseUrl}/billing?checkout=success")
            .setCancelUrl("${properties.returnBaseUrl}/billing?checkout=cancelled")
            .build()
        return CheckoutSession.create(params).url
    }

    /** Opens the Stripe Customer Portal: change or cancel the plan, update the card, see invoices. */
    fun portal(userId: Long): String {
        requireConfigured()
        val user = userService.findEntity(userId)
        val customer = user.stripeCustomerId
            ?: throw AlreadySubscribedException("There is no subscription to manage yet")
        val params = PortalSessionParams.builder()
            .setCustomer(customer)
            .setConfiguration(portalConfiguration())
            .setReturnUrl("${properties.returnBaseUrl}/billing")
            .build()
        return PortalSession.create(params).url
    }

    // -----------------------------------------------------------------------
    // Webhooks
    // -----------------------------------------------------------------------

    /**
     * Handles a webhook from Stripe. The signature is verified first, and each
     * event is processed once even if Stripe delivers it again.
     */
    @Transactional
    fun handleWebhook(payload: String, signature: String?) {
        if (properties.webhookSecret.isBlank()) throw PaymentsDisabledException("STRIPE_WEBHOOK_SECRET is not set")
        val event = try {
            Webhook.constructEvent(payload, signature, properties.webhookSecret)
        } catch (e: SignatureVerificationException) {
            throw InvalidWebhookException("Invalid Stripe signature")
        }
        if (stripeEventRepository.existsByEventId(event.id)) return

        when (event.type) {
            "checkout.session.completed" -> onCheckoutCompleted(dataObject(event) as CheckoutSession)
            "customer.subscription.created", "customer.subscription.updated" -> applySubscription(dataObject(event) as Subscription)
            "customer.subscription.deleted" -> subscriptionService.endStripeSubscription((dataObject(event) as Subscription).id)
            "invoice.paid" -> onInvoicePaid(dataObject(event) as Invoice)
            else -> log.debug("Ignoring Stripe event {}", event.type)
        }
        stripeEventRepository.save(StripeEvent(eventId = event.id, type = event.type))
    }

    private fun onCheckoutCompleted(session: CheckoutSession) {
        val userId = session.metadata["userId"]?.toLongOrNull() ?: return
        val pack = session.metadata["pack"]?.let { runCatching { TokenPack.valueOf(it) }.getOrNull() }
        if (session.mode == "payment" && pack != null && session.paymentStatus == "paid") {
            tokenService.purchaseTokens(userId, pack)
            log.info("User {} bought the {} token pack", userId, pack)
        }
        // Subscriptions are applied from their own events and the first invoice.
    }

    /** The first invoice or a renewal was paid: grant the month's tokens. */
    private fun onInvoicePaid(invoice: Invoice) {
        val subscriptionId = invoice.parent?.subscriptionDetails?.subscription ?: return
        // Events can arrive in any order, so make sure the subscription (and tier) is applied first.
        val userId = applySubscription(Subscription.retrieve(subscriptionId)) ?: return
        subscriptionService.stripeInvoicePaid(userId, invoice.billingReason)
    }

    /** Mirrors a Stripe subscription into our tiers. Returns the user it belongs to. */
    private fun applySubscription(subscription: Subscription): Long? {
        val userId = subscription.metadata["userId"]?.toLongOrNull()
            ?: userRepository.findByStripeCustomerId(subscription.customer)?.id
            ?: return null.also { log.warn("Stripe subscription {} has no known user", subscription.id) }
        if (subscription.status !in ACTIVE_STATUSES) return userId

        val item = subscription.items.data.first()
        val tier = tierForLookupKey(item.price.lookupKey) ?: return userId
        subscriptionService.applyStripeSubscription(
            userId = userId,
            stripeSubscriptionId = subscription.id,
            tier = tier,
            periodStart = Instant.ofEpochSecond(item.currentPeriodStart),
            periodEnd = Instant.ofEpochSecond(item.currentPeriodEnd),
            cancelAtPeriodEnd = subscription.cancelAtPeriodEnd == true,
        )
        return userId
    }

    private fun dataObject(event: Event) =
        event.dataObjectDeserializer.getObject().orElseGet { event.dataObjectDeserializer.deserializeUnsafe() }

    // -----------------------------------------------------------------------
    // Prices, customers and the portal, created on first use
    // -----------------------------------------------------------------------

    private fun planLookupKey(tier: SubscriptionTier) = "narrion_${tier.name.lowercase()}_monthly"
    private fun packLookupKey(pack: TokenPack) = "narrion_pack_${pack.name.lowercase()}"
    private fun tierForLookupKey(key: String?) = SubscriptionTier.entries.firstOrNull { planLookupKey(it) == key }

    private fun planPrice(tier: SubscriptionTier) =
        price(planLookupKey(tier), "Narrion ${tier.label}", tier.priceEuroCents.toLong(), monthly = true)

    private fun packPrice(pack: TokenPack) =
        price(packLookupKey(pack), "Narrion ${pack.tokens} token pack", pack.priceEuroCents.toLong(), monthly = false)

    /** Finds a price by its lookup key, creating it (and its product) the first time. */
    private fun price(lookupKey: String, name: String, amountCents: Long, monthly: Boolean): Price =
        prices.getOrPut(lookupKey) {
            Price.list(PriceListParams.builder().addLookupKey(lookupKey).setActive(true).build()).data.firstOrNull()
                ?: Price.create(
                    PriceCreateParams.builder()
                        .setLookupKey(lookupKey)
                        .setCurrency("eur")
                        .setUnitAmount(amountCents)
                        .setProductData(PriceCreateParams.ProductData.builder().setName(name).build())
                        .apply {
                            if (monthly) {
                                setRecurring(
                                    PriceCreateParams.Recurring.builder()
                                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                                        .build(),
                                )
                            }
                        }
                        .build(),
                ).also { log.info("Created Stripe price {} ({})", lookupKey, it.id) }
        }

    /** The user's Stripe customer, created on their first checkout. */
    private fun customerId(user: User): String =
        user.stripeCustomerId ?: Customer.create(
            CustomerCreateParams.builder()
                .setEmail(user.email)
                .setName(user.displayName)
                .putMetadata("userId", user.id.toString())
                .build(),
        ).id.also {
            user.stripeCustomerId = it
            userRepository.save(user)
        }

    /**
     * The Customer Portal settings: switch between the three plans (upgrades now,
     * downgrades at the end of the period), cancel at the end of the period,
     * update the payment method and see invoices.
     */
    private fun portalConfiguration(): String = portalConfigurationId ?: run {
        val existing = Configuration.list(ConfigurationListParams.builder().setActive(true).setLimit(100L).build())
            .data.firstOrNull { it.metadata["app"] == PORTAL_TAG }
        val id = existing?.id ?: createPortalConfiguration()
        portalConfigurationId = id
        id
    }

    private fun createPortalConfiguration(): String {
        val plans = SubscriptionTier.entries.filter { it != SubscriptionTier.FREE }.map(::planPrice)
        val features = ConfigurationCreateParams.Features.builder()
            .setSubscriptionUpdate(
                ConfigurationCreateParams.Features.SubscriptionUpdate.builder()
                    .setEnabled(true)
                    .addDefaultAllowedUpdate(ConfigurationCreateParams.Features.SubscriptionUpdate.DefaultAllowedUpdate.PRICE)
                    .setProrationBehavior(ConfigurationCreateParams.Features.SubscriptionUpdate.ProrationBehavior.ALWAYS_INVOICE)
                    .setScheduleAtPeriodEnd(
                        ConfigurationCreateParams.Features.SubscriptionUpdate.ScheduleAtPeriodEnd.builder()
                            .addCondition(
                                ConfigurationCreateParams.Features.SubscriptionUpdate.ScheduleAtPeriodEnd.Condition.builder()
                                    .setType(
                                        ConfigurationCreateParams.Features.SubscriptionUpdate.ScheduleAtPeriodEnd.Condition.Type
                                            .DECREASING_ITEM_AMOUNT,
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    .apply {
                        plans.groupBy { it.product }.forEach { (product, prices) ->
                            addProduct(
                                ConfigurationCreateParams.Features.SubscriptionUpdate.Product.builder()
                                    .setProduct(product)
                                    .addAllPrice(prices.map { it.id })
                                    .build(),
                            )
                        }
                    }
                    .build(),
            )
            .setSubscriptionCancel(
                ConfigurationCreateParams.Features.SubscriptionCancel.builder()
                    .setEnabled(true)
                    .setMode(ConfigurationCreateParams.Features.SubscriptionCancel.Mode.AT_PERIOD_END)
                    .build(),
            )
            .setPaymentMethodUpdate(ConfigurationCreateParams.Features.PaymentMethodUpdate.builder().setEnabled(true).build())
            .setInvoiceHistory(ConfigurationCreateParams.Features.InvoiceHistory.builder().setEnabled(true).build())
            .build()

        return Configuration.create(
            ConfigurationCreateParams.builder()
                .setFeatures(features)
                .setDefaultReturnUrl("${properties.returnBaseUrl}/billing")
                .putMetadata("app", PORTAL_TAG)
                .build(),
        ).id.also { log.info("Created Stripe customer portal configuration {}", it) }
    }

    private fun requireConfigured() {
        if (!properties.isConfigured) throw PaymentsDisabledException("Payments are not set up yet")
    }

    private companion object {
        const val PORTAL_TAG = "narrion"
        val ACTIVE_STATUSES = setOf("active", "trialing", "past_due")
    }
}

/** Payments are not configured (no STRIPE_SECRET_KEY). */
class PaymentsDisabledException(message: String) : RuntimeException(message)

/** A second subscription was requested, or there is nothing to manage yet. */
class AlreadySubscribedException(message: String) : RuntimeException(message)

/** The webhook did not come from Stripe. */
class InvalidWebhookException(message: String) : RuntimeException(message)
