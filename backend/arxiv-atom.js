/**
 * Extract one XML/Atom element's text, including multiline content and optional attributes.
 * This is a deliberately small tag extractor for the controlled arXiv Atom response;
 * it is not a general-purpose XML parser.
 */
export function extractAtomTag(xml, tag) {
  if (typeof xml !== "string" || !/^[A-Za-z][A-Za-z0-9:_-]*$/.test(tag)) return "";
  const escapedTag = tag.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = xml.match(new RegExp("<" + escapedTag + "(?:\\s[^>]*)?>([\\s\\S]*?)<\\/" + escapedTag + ">", "i"));
  return match?.[1] || "";
}
