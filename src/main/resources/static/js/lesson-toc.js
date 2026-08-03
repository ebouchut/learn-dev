/*
 * Enhances the lesson table of contents (ADR-0018).
 *
 * The TOC itself is server-rendered and sticky by CSS; this script only
 * adds what the server cannot know: it opens the panel on wide screens
 * (the markup ships closed, the right mobile-first floor) and tracks
 * the reader's position, marking the current section's TOC link with
 * aria-current so visual and assistive state stay one and the same.
 */
(function () {
  "use strict";

  var toc = document.querySelector(".lesson-toc");
  if (!toc) {
    return;
  }

  var wideScreen = window.matchMedia("(min-width: 46rem)");
  if (wideScreen.matches) {
    toc.open = true;
  }

  // On narrow viewports the open panel is pinned over the content, so
  // picking a section must collapse it or it hides the very heading
  // the reader jumped to. Collapsing also shifts the layout, and the
  // browser's own anchor scroll loses that race, so the jump is driven
  // here: collapse first, then scroll from the settled layout. The
  // desktop rail never overlaps and keeps the default behavior.
  // Evaluated at click time: the viewport may have changed.
  toc.addEventListener("click", function (event) {
    var link = event.target.closest(".lesson-toc__link");
    if (!link || wideScreen.matches) {
      return;
    }
    var heading = document.getElementById(decodeURIComponent(link.hash.slice(1)));
    if (!heading) {
      return;
    }
    event.preventDefault();
    toc.open = false;
    history.pushState(null, "", link.hash);
    heading.scrollIntoView({ block: "start" });
  });

  var sections = [];
  toc.querySelectorAll(".lesson-toc__link").forEach(function (link) {
    var id = decodeURIComponent(link.hash.slice(1));
    var heading = document.getElementById(id);
    if (heading) {
      sections.push({ link: link, heading: heading });
    }
  });
  if (sections.length === 0) {
    return;
  }

  var current = null;

  // The observer fires as headings cross the reading zone; the actual
  // pick scans positions, which stays correct in both scroll directions.
  var observer = new IntersectionObserver(markCurrent, {
    rootMargin: "0px 0px -55% 0px"
  });
  sections.forEach(function (section) {
    observer.observe(section.heading);
  });
  // Smooth scrolling can settle after the last threshold crossing;
  // scrollend re-runs the pick at the final position where supported.
  window.addEventListener("scrollend", markCurrent);
  markCurrent();

  function markCurrent() {
    var line = window.innerHeight * 0.35;
    var active = sections[0];
    sections.forEach(function (section) {
      if (section.heading.getBoundingClientRect().top <= line) {
        active = section;
      }
    });
    if (current === active) {
      return;
    }
    if (current) {
      current.link.removeAttribute("aria-current");
    }
    active.link.setAttribute("aria-current", "true");
    current = active;
  }
})();
