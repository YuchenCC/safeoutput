(function (window) {
  function nanosToMs(value) {
    const number = Number(value);
    if (!isFinite(number)) {
      return '0.000 ms';
    }
    return (Math.max(0, number) / 1000000).toFixed(3) + ' ms';
  }

  function elapsedKey(key) {
    if (key === 'elapsedNanos') {
      return 'elapsedMs';
    }
    return key.replace(/ElapsedNanos$/, 'ElapsedMs');
  }

  function toDisplayTiming(value) {
    if (Array.isArray(value)) {
      return value.map(toDisplayTiming);
    }
    if (value && typeof value === 'object') {
      return Object.keys(value).reduce(function (target, key) {
        const displayKey = /ElapsedNanos$/.test(key) || key === 'elapsedNanos' ? elapsedKey(key) : key;
        target[displayKey] = displayKey === key ? toDisplayTiming(value[key]) : nanosToMs(value[key]);
        return target;
      }, {});
    }
    return value;
  }

  window.SafeOutputFormat = {
    nanosToMs: nanosToMs,
    toDisplayTiming: toDisplayTiming
  };
})(window);
