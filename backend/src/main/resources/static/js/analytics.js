/*!
 * Analytics snippet - lightweight, privacy-friendly event collection.
 *
 * Usage:
 *   <script defer src="https://your-api/js/analytics.js"
 *           data-key="pk_xxxxxxxx"
 *           data-endpoint="https://your-api/api/v1/events"
 *           data-auto-click="false"
 *           data-outbound="false"
 *           data-download="false"
 *           data-images="false"
 *           data-sections="false"
 *           data-scroll="true"
 *           data-heatmap-move="false"
 *           data-consent="false"
 *           data-sample="1"></script>
 *
 * Custom events:
 *   window.analytics.track("signup", { plan: "pro" });
 *
 * Labeled elements (default click tracking):
 *   <button data-analytics="signup">...</button>   -> event "signup"
 *   <a data-analytics>...</a>                       -> event "click"
 *
 * Consent:
 *   window.analytics.consent(true);   // start collecting (when data-consent="true")
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

  function attr(name, fallback) {
    var value = script && script.getAttribute(name);
    if (value === null || value === undefined) {
      return fallback;
    }
    return value;
  }

  var CONFIG = {
    autoClick: attr("data-auto-click", "false") === "true",
    outbound: attr("data-outbound", "false") === "true",
    download: attr("data-download", "false") === "true",
    images: attr("data-images", "false") === "true",
    sections: attr("data-sections", "false") === "true",
    scroll: attr("data-scroll", "true") !== "false",
    heatmapMove: attr("data-heatmap-move", "false") === "true",
    consentRequired: attr("data-consent", "false") === "true",
    sample: parseFloat(attr("data-sample", "1")) || 1
  };

  var DOMAIN = null;
  try {
    DOMAIN = new URL(script && script.src ? script.src : location.href).hostname;
  } catch (e) {
    DOMAIN = location.hostname;
  }

  // Context keys that the backend stores in dedicated columns (must go at the
  // top level of the event, not inside "properties").
  var CONTEXT_KEYS = [
    "elementTag", "elementSelector", "elementText", "elementId", "href",
    "clickX", "clickY", "clickXPct", "clickYPct",
    "viewportWidth", "viewportHeight", "pageHeight",
    "durationMs", "engagedMs", "scrollPct",
    "imageKey", "imageAlt", "dwellMs", "sectionKey"
  ];

  var VISITOR_KEY = "an_visitor";
  var SESSION_KEY = "an_session";
  var SESSION_TS_KEY = "an_session_ts";
  var SESSION_TIMEOUT_MS = 30 * 60 * 1000;
  var MAX_BATCH = 100;
  var queue = [];
  var lastUrl = null;
  var flushTimer = null;

  var started = !CONFIG.consentRequired;
  var pageStart = Date.now();
  var hiddenSince = null;
  var hiddenTotal = 0;
  var maxScroll = 0;
  var leaveSent = false;
  var moveBuf = [];
  var lastMoveAt = 0;
  var seenImages = {};
  var sectionTimers = {};
  var sectionTotals = {};

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

  function baseEvent(name) {
    return {
      name: name,
      url: window.location.href,
      referrer: document.referrer || null,
      screenWidth: window.screen ? window.screen.width : null,
      screenHeight: window.screen ? window.screen.height : null,
      language: navigator.language || null,
      visitorId: visitorId(),
      sessionId: sessionId(),
      properties: {}
    };
  }

  function scheduleFlush(delay) {
    if (flushTimer) {
      return;
    }
    flushTimer = setTimeout(function () {
      flushTimer = null;
      flush();
    }, delay === undefined ? 1000 : delay);
  }

  function enqueue(event) {
    if (!started) {
      return;
    }
    queue.push(event);
    if (queue.length >= MAX_BATCH) {
      flush();
      return;
    }
    scheduleFlush();
  }

  function sendBatch(events) {
    var payload = JSON.stringify({ events: events });
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

  function flush() {
    if (!queue.length) {
      return;
    }
    var pending = queue;
    queue = [];
    for (var i = 0; i < pending.length; i += MAX_BATCH) {
      sendBatch(pending.slice(i, i + MAX_BATCH));
    }
  }

  function trackPageview() {
    if (window.location.href === lastUrl) {
      return;
    }
    lastUrl = window.location.href;
    pageStart = Date.now();
    leaveSent = false;
    enqueue(baseEvent("pageview"));
  }

  // Public API: arbitrary user data goes under "properties".
  function track(name, properties) {
    var event = baseEvent(name);
    event.properties = properties || {};
    enqueue(event);
  }

  // Internal: known context fields go at the top level, so the backend stores
  // them in dedicated columns. Unknown keys fall back into "properties".
  function contextEvent(name, fields) {
    var event = baseEvent(name);
    for (var key in fields) {
      if (!Object.prototype.hasOwnProperty.call(fields, key)) {
        continue;
      }
      var value = fields[key];
      if (value === undefined || value === null) {
        continue;
      }
      if (CONTEXT_KEYS.indexOf(key) !== -1) {
        event[key] = value;
      } else {
        event.properties[key] = value;
      }
    }
    enqueue(event);
  }

  function scrollPct() {
    var doc = document.documentElement;
    var height = Math.max(doc.scrollHeight, document.body ? document.body.scrollHeight : 0);
    if (height <= 0) {
      return 0;
    }
    var current = window.scrollY + window.innerHeight;
    var pct = (current / height) * 100;
    return Math.max(0, Math.min(100, Math.round(pct)));
  }

  function updateScroll() {
    var pct = scrollPct();
    if (pct > maxScroll) {
      maxScroll = pct;
    }
  }

  function trackPageleave() {
    if (leaveSent) {
      return;
    }
    leaveSent = true;
    var now = Date.now();
    var durationMs = now - pageStart;
    var hidden = hiddenTotal + (hiddenSince ? now - hiddenSince : 0);
    var engagedMs = Math.max(0, durationMs - hidden);
    contextEvent("pageleave", {
      durationMs: durationMs,
      engagedMs: engagedMs,
      scrollPct: maxScroll
    });
    flushSections();
    flushMoves();
    flush();
  }

  function cssPath(el) {
    if (!el || el.nodeType !== 1) {
      return null;
    }
    if (el.id) {
      return "#" + el.id;
    }
    var parts = [];
    var node = el;
    var depth = 0;
    while (node && node.nodeType === 1 && depth < 6) {
      var part = node.nodeName.toLowerCase();
      if (node.id) {
        part = "#" + node.id;
        parts.unshift(part);
        break;
      }
      var cls = (node.getAttribute("class") || "").trim().split(/\s+/).filter(Boolean)[0];
      if (cls) {
        part += "." + cls;
      }
      var parent = node.parentNode;
      if (parent) {
        var siblings = [];
        var child = parent.firstElementChild;
        while (child) {
          if (child.nodeName === node.nodeName) {
            siblings.push(child);
          }
          child = child.nextElementSibling;
        }
        if (siblings.length > 1) {
          part += ":nth-of-type(" + (siblings.indexOf(node) + 1) + ")";
        }
      }
      parts.unshift(part);
      node = node.parentNode;
      depth++;
    }
    var path = parts.join(" > ");
    return path.length > 1024 ? path.slice(0, 1024) : path;
  }

  function elementText(el) {
    var text = (el.innerText || el.textContent || "").replace(/\s+/g, " ").trim();
    return text ? text.slice(0, 120) : null;
  }

  function clickContext(el, ev) {
    var ctx = {
      elementTag: el.nodeName ? el.nodeName.toLowerCase().slice(0, 32) : null,
      elementSelector: cssPath(el),
      elementText: elementText(el),
      elementId: el.id ? el.id.slice(0, 255) : null,
      href: el.getAttribute && el.getAttribute("href") ? String(el.getAttribute("href")).slice(0, 4096) : null,
      viewportWidth: window.innerWidth,
      viewportHeight: window.innerHeight,
      pageHeight: Math.max(document.documentElement.scrollHeight, document.body ? document.body.scrollHeight : 0)
    };
    if (ev && typeof ev.clientX === "number") {
      var pageHeight = ctx.pageHeight || 1;
      var pageY = ev.pageY !== undefined ? ev.pageY : ev.clientY + window.scrollY;
      ctx.clickX = Math.round(ev.clientX);
      ctx.clickY = Math.round(pageY);
      ctx.clickXPct = Math.round((ev.clientX / (window.innerWidth || 1)) * 100000) / 1000;
      ctx.clickYPct = Math.round((pageY / pageHeight) * 100000) / 1000;
    }
    return ctx;
  }

  function merge(base, extra) {
    for (var key in extra) {
      if (Object.prototype.hasOwnProperty.call(extra, key) && extra[key] !== undefined && extra[key] !== null) {
        base[key] = extra[key];
      }
    }
    return base;
  }

  function isDownloadLink(a) {
    if (!a || !a.getAttribute) {
      return false;
    }
    if (a.hasAttribute("download")) {
      return true;
    }
    var href = (a.getAttribute("href") || "").split("?")[0].toLowerCase();
    return /\.(pdf|zip|rar|7z|csv|xls|xlsx|doc|docx|ppt|pptx|txt|rtf|mp3|mp4|mov|avi|wav|dmg|exe|apk|epub)$/.test(href);
  }

  function onDocumentClick(ev) {
    var target = ev.target;
    if (!target || target.nodeType !== 1) {
      return;
    }
    var labeled = target.closest ? target.closest("[data-analytics]") : null;
    var anchor = target.closest ? target.closest("a[href]") : null;
    var handled = false;

    if (labeled) {
      var label = labeled.getAttribute("data-analytics");
      var name = label && label.trim() ? label.trim() : "click";
      contextEvent(name, clickContext(labeled, ev));
      handled = true;
    } else if (CONFIG.autoClick) {
      contextEvent("click", clickContext(target, ev));
      handled = true;
    }

    if (anchor) {
      var href = anchor.getAttribute("href") || "";
      if (CONFIG.download && isDownloadLink(anchor)) {
        contextEvent("file_download", merge(clickContext(anchor, ev), { href: href }));
        handled = true;
      }
      if (CONFIG.outbound) {
        try {
          var host = new URL(anchor.href, location.href).hostname;
          if (host && host !== location.hostname && host !== DOMAIN) {
            contextEvent("outbound_link", merge(clickContext(anchor, ev), { href: href }));
            handled = true;
          }
        } catch (e) {}
      }
    }

    if (handled) {
      scheduleFlush(0);
    }
  }

  function imageKeyOf(img) {
    var src = img.currentSrc || img.src;
    if (!src) {
      return null;
    }
    try {
      var url = new URL(src, location.href);
      return (url.origin + url.pathname).slice(0, 1024);
    } catch (e) {
      return src.slice(0, 1024);
    }
  }

  function observeImages() {
    if (!("IntersectionObserver" in window)) {
      return;
    }
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        var img = entry.target;
        var key = imageKeyOf(img);
        if (!key || seenImages[key]) {
          return;
        }
        if (entry.isIntersecting) {
          img.__anVisibleSince = Date.now();
        } else if (img.__anVisibleSince) {
          var dwell = Date.now() - img.__anVisibleSince;
          img.__anVisibleSince = null;
          if (dwell >= 1000) {
            seenImages[key] = true;
            contextEvent("image_view", {
              imageKey: key,
              imageAlt: img.getAttribute("alt") ? img.getAttribute("alt").slice(0, 512) : null,
              dwellMs: dwell
            });
          }
        }
      });
    }, { threshold: 0.5 });
    Array.prototype.forEach.call(document.images || [], function (img) {
      observer.observe(img);
    });
  }

  function observeSections() {
    if (!("IntersectionObserver" in window)) {
      return;
    }
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        var key = entry.target.getAttribute("data-analytics-section") || cssPath(entry.target);
        if (!key) {
          return;
        }
        if (entry.isIntersecting) {
          sectionTimers[key] = Date.now();
        } else if (sectionTimers[key]) {
          var dwell = Date.now() - sectionTimers[key];
          sectionTimers[key] = null;
          sectionTotals[key] = (sectionTotals[key] || 0) + dwell;
        }
      });
    }, { threshold: 0.25 });
    Array.prototype.forEach.call(document.querySelectorAll("[data-analytics-section]"), function (el) {
      observer.observe(el);
    });
  }

  function flushSections() {
    var now = Date.now();
    Object.keys(sectionTotals).forEach(function (key) {
      var active = sectionTimers[key] ? now - sectionTimers[key] : 0;
      var total = (sectionTotals[key] || 0) + active;
      if (total >= 200) {
        contextEvent("section_engage", { sectionKey: key, durationMs: total, scrollPct: maxScroll });
      }
    });
    sectionTotals = {};
    sectionTimers = {};
  }

  function onMouseMove(ev) {
    if (!started || !CONFIG.heatmapMove) {
      return;
    }
    var now = Date.now();
    if (now - lastMoveAt < 250 || moveBuf.length >= 200) {
      return;
    }
    lastMoveAt = now;
    var el = ev.target && ev.target.nodeType === 1 ? ev.target : document.body;
    moveBuf.push(clickContext(el, ev));
  }

  function flushMoves() {
    moveBuf.forEach(function (fields) {
      contextEvent("mousemove", fields);
    });
    moveBuf = [];
  }

  function startCollectors() {
    if (CONFIG.scroll) {
      window.addEventListener("scroll", updateScroll, { passive: true });
      updateScroll();
    }
    document.addEventListener("click", onDocumentClick, true);
    window.addEventListener("mousemove", onMouseMove, { passive: true });
    if (CONFIG.images) {
      observeImages();
    }
    if (CONFIG.sections) {
      observeSections();
    }
  }

  window.analytics = {
    track: track,
    pageview: trackPageview,
    flush: flush,
    consent: function (granted) {
      if (granted && !started) {
        started = true;
        startCollectors();
        trackPageview();
        scheduleFlush(0);
      } else if (!granted) {
        started = false;
      }
    }
  };

  var pushState = history.pushState;
  history.pushState = function () {
    pushState.apply(history, arguments);
    setTimeout(function () {
      trackPageleave();
      trackPageview();
    }, 0);
  };
  window.addEventListener("popstate", function () {
    setTimeout(function () {
      trackPageleave();
      trackPageview();
    }, 0);
  });
  window.addEventListener("pagehide", trackPageleave);
  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "hidden") {
      hiddenSince = Date.now();
      trackPageleave();
    } else if (hiddenSince) {
      hiddenTotal += Date.now() - hiddenSince;
      hiddenSince = null;
    }
  });

  if (started) {
    if (document.readyState === "complete") {
      trackPageview();
      startCollectors();
    } else {
      window.addEventListener("load", function () {
        trackPageview();
        startCollectors();
      });
    }
  }
})();
