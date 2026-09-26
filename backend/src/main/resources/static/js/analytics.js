/*!
 * Analytics snippet - lightweight, privacy-friendly event collection.
 *
 * Usage:
 *   <script defer src="https://your-api/js/analytics.js"
 *           data-key="pk_xxxxxxxx"
 *           data-endpoint="https://your-api/api/v1/events"></script>
 *
 * Custom events:
 *   window.analytics.track("signup", { plan: "pro" });
 */
(function () {
  "use strict";

  var script = document.currentScript || (function () {
    var scripts = document.getElementsByTagName("script");
    return scripts[scripts.length - 1];
  })();

  var apiKey = script && script.getAttribute("data-key");
  var endpoint = script && script.getAttribute("data-endpoint");
  if (apiKey && apiKey.indexOf("pk_") === 0) {
    apiKey = encodeURIComponent(apiKey);
  }
  if (!endpoint && script && script.src) {
    try {
      endpoint = new URL(script.src).origin + "/api/v1/events";
    } catch (e) {
      endpoint = "/api/v1/events";
    }
  }
  if (!apiKey) {
    return;
  }

  var VISITOR_KEY = "an_visitor";
  var SESSION_KEY = "an_session";
  var SESSION_TS_KEY = "an_session_ts";
  var SESSION_TIMEOUT_MS = 30 * 60 * 1000;
  var queue = [];
  var lastUrl = null;
  var flushTimer = null;

  function uuid() {
    if (window.crypto && window.crypto.randomUUID) {
      return window.crypto.randomUUID();
    }
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
      var r = (Math.random() * 16) | 0;
      var v = c === "x" ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  function store(key, value) {
    try { localStorage.setItem(key, value); } catch (e) {}
  }

  function read(key) {
    try { return localStorage.getItem(key); } catch (e) { return null; }
  }

  function visitorId() {
    var id = read(VISITOR_KEY);
    if (!id) {
      id = uuid();
      store(VISITOR_KEY, id);
    }
    return id;
  }

  function sessionId() {
    var now = Date.now();
    var last = parseInt(read(SESSION_TS_KEY) || "0", 10);
    var id = read(SESSION_KEY);
    if (!id || now - last > SESSION_TIMEOUT_MS) {
      id = uuid();
      store(SESSION_KEY, id);
    }
    store(SESSION_TS_KEY, String(now));
    return id;
  }

  function baseEvent(name, properties) {
    return {
      name: name,
      url: window.location.href,
      referrer: document.referrer || null,
      screenWidth: window.screen ? window.screen.width : null,
      screenHeight: window.screen ? window.screen.height : null,
      language: navigator.language || null,
      visitorId: visitorId(),
      sessionId: sessionId(),
      properties: properties || {}
    };
  }

  function scheduleFlush() {
    if (flushTimer) {
      return;
    }
    flushTimer = setTimeout(function () {
      flushTimer = null;
      flush();
    }, 1000);
  }

  function enqueue(event) {
    queue.push(event);
    scheduleFlush();
  }

  function flush() {
    if (!queue.length) {
      return;
    }
    var payload = JSON.stringify({ events: queue });
    queue = [];
    var url = endpoint + (endpoint.indexOf("?") === -1 ? "?" : "&") + "k=" + apiKey;
    try {
      if (navigator.sendBeacon) {
        var blob = new Blob([payload], { type: "application/json" });
        if (navigator.sendBeacon(url, blob)) {
          return;
        }
      }
    } catch (e) {}
    try {
      fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Api-Key": apiKey },
        body: payload,
        keepalive: true
      });
    } catch (e) {}
  }

  function trackPageview() {
    if (window.location.href === lastUrl) {
      return;
    }
    lastUrl = window.location.href;
    enqueue(baseEvent("pageview", {}));
  }

  function track(name, properties) {
    enqueue(baseEvent(name, properties));
  }

  window.analytics = { track: track, pageview: trackPageview, flush: flush };

  var pushState = history.pushState;
  history.pushState = function () {
    pushState.apply(history, arguments);
    setTimeout(trackPageview, 0);
  };
  window.addEventListener("popstate", function () {
    setTimeout(trackPageview, 0);
  });
  window.addEventListener("pagehide", flush);
  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "hidden") {
      flush();
    }
  });

  if (document.readyState === "complete") {
    trackPageview();
  } else {
    window.addEventListener("load", trackPageview);
  }
})();
