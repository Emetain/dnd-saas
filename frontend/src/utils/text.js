/** "questGiverName" or "quest_giver" → "Quest Giver Name" / "Quest giver". */
export function humanize(key) {
  return key
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/_/g, ' ')
    .replace(/^./, (c) => c.toUpperCase())
}

const isObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v)
const isEmpty = (v) => v == null || v === '' || (Array.isArray(v) && v.length === 0)

/**
 * A generator result as plain text, for the Copy button: labelled fields,
 * nested lists indented, empty fields left out.
 */
export function toPlainText(value, indent = '') {
  if (isEmpty(value)) return ''
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (Array.isArray(value)) {
    return value
      .map((item) => {
        if (!isObject(item)) return `${indent}- ${item}`
        const title = item.name || item.title
        const rest = Object.fromEntries(Object.entries(item).filter(([k]) => k !== 'name' && k !== 'title'))
        const body = toPlainText(rest, `${indent}  `)
        return title ? `${indent}- ${title}\n${body}` : `${indent}-\n${body}`
      })
      .join('\n')
  }
  if (isObject(value)) {
    return Object.entries(value)
      .filter(([, v]) => !isEmpty(v))
      .map(([k, v]) =>
        typeof v === 'object' ? `${indent}${humanize(k)}:\n${toPlainText(v, `${indent}  `)}` : `${indent}${humanize(k)}: ${toPlainText(v)}`,
      )
      .join('\n')
  }
  return String(value)
}

/** A showcase example as plain text, for the Copy button. */
export function exampleToText(example) {
  return [
    example.name,
    example.label,
    ...example.fields.map((f) => `${f.label}: ${f.value}`),
    example.settings,
  ].join('\n')
}
