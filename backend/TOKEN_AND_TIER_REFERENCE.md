#!/Users/jay/IdeaProjects/dnd-saas/backend/TOKEN_AND_TIER_REFERENCE.md

# Token & Subscription Tier Reference

## Platform Token Costs (from GenerationType enum)

| Generator | Tokens | Tiers | Notes |
|-----------|---------|-------|-------|
| Campaign | 10 | Pro, Pro+, Ultimate | Most expensive - full campaign |
| One-Shot | 8 | Pro, Pro+, Ultimate | Compact adventure |
| Encounter | 2 | All | Structured encounter data |
| Quest | 2 | All | Quest for campaign |
| Boss | 3 | All | Boss creature with tactics |
| Backstory | 2 | All | PC backstory |
| Shop | 2 | All | Item shop with inventory |
| Puzzle | 2 | All | Challenge/puzzle |
| NPC | 1 | All | Single NPC |
| Loot | 1 | All | Treasure drops |
| Random Encounter | 1 | All | Random combat |
| Freeform | 1 | All | Q&A with campaign context |

## Subscription Tiers

### FREE
- **Monthly Tokens**: 50
- **Starting Tokens**: 50 (on account creation)
- **Includes**:
  - Account creation
  - Basic generators (all types at full token cost)
  - Ad watching (earn tokens)
  - Referral program (earn tokens)
  - Daily login streaks (earn tokens)
  - App reviews (earn tokens)
  - Access to campaign memory
  - Limited AI context injection
- **Does NOT include**:
  - Unlimited generators
  - Save & continue (limited by token balance)
  - Future: Premium image/map generation

**Example**: Free user with 50 tokens can:
- Generate 50 random encounters (1 token each)
- Generate 5 one-shots (8 tokens each) + 10 random encounters
- Generate 1 campaign (10 tokens) + 5 random encounters

### PRO
- **Monthly Tokens**: 1,000
- **Includes**: Everything FREE, plus
  - Save and continue campaigns
  - Access to standard generators
  - Campaign Memory persistence
  - More context for AI requests
  - Ability to purchase additional tokens

**Example**: Pro user gets 1,000 tokens/month = ~100 one-shots or ~10 campaigns

### PRO+
- **Monthly Tokens**: 3,000
- **Includes**: Everything PRO, plus
  - Unlimited standard generator usage (subject to tokens)
  - Higher token allowance

**Example**: Pro+ user gets 3,000 tokens/month = ~300 one-shots or ~30 campaigns

### ULTIMATE DM
- **Monthly Tokens**: 5,000
- **Includes**: Everything PRO+, plus
  - Premium image generation (NPC portraits, locations, scenes)
  - World map generation
  - Region map generation
  - City map generation
  - Dungeon map generation
  - Battle/encounter map generation
  - Future premium DM tools

**Note**: Image/map generation rates TBD in Phase 8

## Free Tier Token Earning

| Source | Per Action | Limit | Monthly Potential |
|--------|-----------|-------|------------------|
| Ad Viewing | 5 tokens | 10/day | ~1,500 (30×10×5) |
| Login Streak | 1 token | Daily | ~30 (1/day×30 days) |
| Login Streak Bonus | +25 tokens | Every 7 days | ~100 (4 weeks×25) |
| Referral | 50 tokens (referrer) | Unlimited | Depends on friends |
| Referral | 25 tokens (referred) | Once per user | One-time |
| App Review | 25 tokens | Once per platform | ~50 (2 platforms max) |
| Social Share | 10 tokens | 2/day | ~600 (30×2×10) |
| **TOTAL** | | | **~2,280** |

### How Free Users Can Fund Themselves

**Realistic Monthly Earning Path:**
1. Login daily (30 tokens + 100 bonus) = +130 tokens
2. Watch 5 ads per day (5×5×30) = +750 tokens
3. Refer 1 friend who signs up = +50 tokens
4. Get 2 friends to review apps = +50 tokens
5. Share once a week (4×10) = +40 tokens
6. **Total**: ~1,020 tokens/month

This allows a free user to approximately:
- Generate 1-2 campaigns monthly
- Generate 10-15 one-shots monthly
- Generate 100+ random encounters monthly
- Build within campaign memory persistently

**Incentive**: Using the site creates token earnings that incentivize continued usage and potential upgrade to paid tier.

## Business Model

### Customer Lifecycle

```
Free User (50 tokens)
    ↓ (tries generator, sees cost)
Earns tokens via free actions (+1,000/month max)
    ↓ (wants more/consistent tokens)
Upgrades to Pro (+1,000/month)
    ↓ (growing campaign, needs features)
Upgrades to Pro+ (+3,000/month)
    ↓ (serious about visuals & world building)
Upgrades to Ultimate DM (+5,000/month + image/maps)
```

### Revenue Drivers

**For Free Tier:**
- Ad serving revenue (CPM model)
- Conversion to paid subscriptions
- Referral incentives create network effects

**For Paid Tiers:**
- Monthly subscription revenue
- Premium features (image/map generation in Phase 8)
- Potential additional token purchases

### Token Velocity Metrics (to track)

1. **Tokens Purchased**: $ revenue ÷ tokens bought
2. **Tokens Burned**: Avg tokens spent per user per month
3. **Burn Rate**: If tokens purchased > tokens burned, user will upgrade
4. **Referral Conversion**: % of referred users who complete actions
5. **Free→Pro Conversion**: % of free users upgrading to paid
6. **LTV**: Lifetime value of a free user (time until upgrade or churn)

## Integration Checklist

- [ ] Campaign creation requires user_id
- [ ] Generator endpoints check token balance before processing
- [ ] GenerationLog creation includes tokenCost from GenerationType
- [ ] Token charge happens BEFORE generation (fail fast)
- [ ] UI shows token balance prominently
- [ ] Free tier users see earning opportunities when balance < cost
- [ ] Login tracking integrated (record login on each API call)
- [ ] Scheduled tasks configured for:
  - [ ] Daily login streak reset
  - [ ] Monthly billing cycle refresh
- [ ] Password hashing implemented (bcrypt, NOT plaintext)
- [ ] Frontend displays token costs on each generator

## Notes for Future Phases

**Phase 6 (Frontend)**:
- Prominent token balance display
- Free tier earning dashboard
- Referral sharing widget
- Subscription tier comparison
- Account settings for upgrade/downgrade

**Phase 8 (Image/Map Generation)**:
- Determine token costs for image generation
- Ensure image generation respects token balance
- Gate image/maps behind subscription tiers

**Phase 7 (Improvements)**:
- Analyze referral effectiveness
- Optimize earning opportunities
- Test pricing sensitivity
- Measure free→paid conversion rate

