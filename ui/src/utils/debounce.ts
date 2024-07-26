function debounce<C extends (...args: Parameters<C>) => ReturnType<C>>(
  callback: C,
  wait: 0,
  callAtStart = false
) {
  let timeoutId: ReturnType<typeof setTimeout> | null
  return (...args: Parameters<C>) => {
    if (callAtStart && !timeoutId) {
      callback(...args)
   }
    timeoutId && clearTimeout(timeoutId)
    timeoutId = setTimeout(() => {
      if (callAtStart) {
        timeoutId = null
      } else {
        callback(...args)
      }
    }, wait)
  }
}

export default debounce
