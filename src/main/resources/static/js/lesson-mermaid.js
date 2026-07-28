/*
 * Upgrades mermaid fences in lesson content to SVG diagrams.
 *
 * The server deliberately ships diagrams as sanitized code blocks (see
 * ADR-0016): this script lazily loads the self-hosted Mermaid bundle only
 * when the page actually contains one, renders each block client-side,
 * and keeps the original code block in the DOM behind a toggle as the
 * no-JavaScript, syntax-error, and screen-reader fallback.
 */
(function () {
  "use strict";

  var sourceBlocks = document.querySelectorAll(
    ".lesson-content pre > code.language-mermaid");
  if (sourceBlocks.length === 0) {
    return;
  }

  var darkScheme = window.matchMedia("(prefers-color-scheme: dark)");
  var renderPass = 0;

  loadMermaid().then(renderAll).catch(function () {
    // The library failed to load: the styled source blocks stay visible.
  });

  darkScheme.addEventListener("change", function () {
    if (window.mermaid) {
      renderAll();
    }
  });

  function loadMermaid() {
    return new Promise(function (resolve, reject) {
      var loader = document.querySelector("script[data-mermaid-src]");
      var script = document.createElement("script");
      script.src = loader.getAttribute("data-mermaid-src");
      script.onload = resolve;
      script.onerror = reject;
      document.head.appendChild(script);
    });
  }

  function renderAll() {
    renderPass += 1;
    window.mermaid.initialize({
      startOnLoad: false,
      securityLevel: "strict",
      theme: darkScheme.matches ? "dark" : "default"
    });
    sourceBlocks.forEach(function (code, index) {
      renderBlock(code.parentElement, code, index);
    });
  }

  function renderBlock(pre, code, index) {
    // A fresh id per pass: Mermaid refuses to render into a used id, and
    // a theme change re-renders every diagram.
    var id = "lesson-mermaid-" + renderPass + "-" + index;
    window.mermaid.render(id, code.textContent)
      .then(function (result) {
        ensureFigure(pre, index).innerHTML = result.svg;
      })
      .catch(function () {
        // Invalid diagram source: the code block stays visible; drop the
        // detached error element Mermaid leaves behind.
        var artifact = document.getElementById("d" + id);
        if (artifact) {
          artifact.remove();
        }
      });
  }

  function ensureFigure(pre, index) {
    var figureId = "lesson-mermaid-figure-" + index;
    var figure = document.getElementById(figureId);
    if (figure) {
      return figure;
    }

    figure = document.createElement("figure");
    figure.id = figureId;
    figure.className = "lesson-mermaid";

    if (!pre.id) {
      pre.id = "lesson-mermaid-source-" + index;
    }
    pre.hidden = true;

    var toggle = document.createElement("button");
    toggle.type = "button";
    toggle.className = "button button--ghost lesson-mermaid__toggle";
    toggle.textContent = "Show diagram source";
    toggle.setAttribute("aria-expanded", "false");
    toggle.setAttribute("aria-controls", pre.id);
    toggle.addEventListener("click", function () {
      var expanded = toggle.getAttribute("aria-expanded") === "true";
      toggle.setAttribute("aria-expanded", String(!expanded));
      toggle.textContent =
        expanded ? "Show diagram source" : "Hide diagram source";
      pre.hidden = expanded;
    });

    pre.parentNode.insertBefore(figure, pre);
    pre.parentNode.insertBefore(toggle, pre);
    return figure;
  }
})();
