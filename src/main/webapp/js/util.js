function accumulateAttributeInChildren(obj, attributeKey, defaultValue) {
  let accumulation = defaultValue;
  for (let [_, child] of attributes(obj)) {
    accumulation += child[attributeKey];
  }
  return accumulation;
}

function attributes(obj) {
    return (for (key of Object.keys(obj)) [key, obj[key]]);
}
