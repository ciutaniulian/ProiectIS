import { useCallback, useEffect, useRef, useState } from 'react'

export function useGroupResource(load, refreshEvery) {
  const [state, setState] = useState({ data: null, error: null })
  const [revision, setRevision] = useState(0)
  const currentRequest = useRef(null)
  const refresh = useCallback(() => {
    currentRequest.current?.abort()
    setRevision(value => value + 1)
  }, [])
  const replace = useCallback(data => {
    currentRequest.current?.abort()
    setState(previous => ({ data: typeof data === 'function' ? data(previous.data) : data, error: null }))
  }, [])
  useEffect(() => {
    const controller = new AbortController()
    currentRequest.current = controller
    load(controller.signal)
      .then(data => {
        if (!controller.signal.aborted) setState({ data, error: null })
      })
      .catch(error => {
        if (!controller.signal.aborted) setState(previous => ({ ...previous, error }))
      })
      .finally(() => {
        if (currentRequest.current === controller) currentRequest.current = null
      })
    return () => {
      controller.abort()
      if (currentRequest.current === controller) currentRequest.current = null
    }
  }, [load, revision])
  useEffect(() => {
    function refreshWhenVisible() {
      if (!document.hidden) refresh()
    }
    window.addEventListener('focus', refreshWhenVisible)
    document.addEventListener('visibilitychange', refreshWhenVisible)
    const interval = refreshEvery ? window.setInterval(() => {
      if (currentRequest.current === null) refreshWhenVisible()
    }, refreshEvery) : null
    return () => {
      window.removeEventListener('focus', refreshWhenVisible)
      document.removeEventListener('visibilitychange', refreshWhenVisible)
      if (interval !== null) window.clearInterval(interval)
    }
  }, [refresh, refreshEvery])
  return { ...state, refresh, replace }
}
