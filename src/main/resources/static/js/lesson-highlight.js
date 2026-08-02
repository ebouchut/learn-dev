/*
 * Colors fenced code blocks in lesson content by language.
 *
 * The server has shipped language-tagged, sanitized code blocks since
 * ADR-0013; this script (ADR-0017) lazily loads the self-hosted
 * highlight.js bundle only when the lesson contains one, and highlights
 * only blocks whose language hint the bundle recognizes. Unknown hints
 * and bare fences stay monochrome on purpose (no auto-detect guessing),
 * and mermaid fences belong to lesson-mermaid.js.
 */
(function () {
  "use strict";

  var codeBlocks = Array.prototype.filter.call(
    document.querySelectorAll(
      '.lesson-content pre > code[class*="language-"]'),
    function (code) {
      return !code.classList.contains("language-mermaid");
    });
  if (codeBlocks.length === 0) {
    return;
  }

  var loader = document.querySelector("script[data-hljs-src]");
  var script = document.createElement("script");
  script.src = loader.getAttribute("data-hljs-src");
  script.onload = highlightAll;
  // On load failure the styled monochrome blocks simply stay.
  document.head.appendChild(script);

  function highlightAll() {
    codeBlocks.forEach(function (code) {
      var hint = /language-([\w-]+)/.exec(code.className);
      if (hint && window.hljs.getLanguage(hint[1])) {
        window.hljs.highlightElement(code);
      }
    });
  }
})();
