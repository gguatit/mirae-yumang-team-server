# STARLOG Main Page Redesign Specification

## Overview
Redesign the STARLOG main page (Thymeleaf template `home.html`) with the existing space/star concept retained but refined with a premium, polished direction. The project uses Spring Boot + Thymeleaf with pure CSS.

## Design Direction
- **Concept**: Retain current space/starlight theme — premium upgrade (Option A)
- **Style**: Flat, typography-driven — no glassmorphism, no purple-blue gradients, no decorative blobs
- **Color**: Project existing palette (`--policeblue`, `--rackley`, `--weldonblue`, `--sliverpink`, `--sky`, `--deep`, `--dark`, `--mid`, `--light`, `--text`, `--accent`)
- **Typography**: `Cafe24ClassicType` throughout; clear hierarchy with letter-spacing for headings
- **Shapes**: `border-radius: 4px-8px` (restrained); no excessive rounding
- **Effects**: Minimal — solid backgrounds, subtle dividers, no blur/glass

## Color System

### Existing Tokens (to use directly)
```
--policeblue: #385479
--rackley: #6B89AB
--weldonblue: #8CA3B5
--sliverpink: #C4BBAB
--sky: #82BEDC
--deep: #161838       (body background)
--dark: #202353       (section background)
--mid: #5C68B2        (card background)
--light: #9DB2F5      (alternate card bg)
--text: #C7D7F9       (body text)
--accent: #FAE8D3     (heading text, CTAs)
```

### Key Changes from Current
- No inline hex values in CSS — use token variables consistently
- Remove `* { color: #C7D7F9 }` global override — use targeted selectors
- Remove `* { font-family: ... }` from home.css (already in reset.css)

## Page Sections

### 1. Hero Section
- Full-viewport Spline 3D background (retain existing)
- Top navigation bar: fixed position, `STARLOG` logo (left), nav links + login/register (right)
- Center: large `STARLOG` heading in `--accent` with wide letter-spacing, decorative divider (`--sky`), subtitle in `--text`, two CTA buttons
- Remove current `spline-logo-overlay` gradient approach — clean transition to next section
- CTA buttons: "시작하기" (filled `--mid`), "별자리 보기" (outlined)

### 2. Intro 0 Section
- Background: `--mid`
- Large quote: "별이 지나간 자리마다, 오늘의 기록이 남습니다"
- Subtext in `--text`
- Centered layout, adequate padding (64px vertical)

### 3. Intro 1 Section (Service)
- Background: `--dark`
- Two-column: image placeholder (left) + descriptive text (right)
- Text describes the service in the existing copy

### 4. Today's Fortune Section
- Background: `--dark` with subtle top border
- Two-column flex layout
- Left: title "오늘의 운세" + description + image placeholder
- Right: card (`background: var(--mid)`, `border-radius: 8px`, padding: 28px)
  - Card title in `--accent`
  - List items with `--sky` dash marker
  - Bottom CTA: "오늘의 운세 대화하기" button (`--dark` background, `--accent` text)

### 5. Intro 2 Section (Community description)
- Same two-column layout as Intro 1 (flipped: text left, image right)
- Background: `--dark`

### 6. Community Section
- Same two-column layout as Today (card left, content right)
- Card has same styling as Today card but community-themed content
- CTA: "학교 커뮤니티 이동하기"

### 7. Footer
- Background: slightly darker than body (`#0E1030`)
- Three-column: logo, copyright, legal links
- Colors: `--rackley`, `--weldonblue`

## Responsive Behavior
- **Desktop (>1200px)**: Horizontal two-column layouts, fixed nav at top
- **Tablet (768-1200px)**: Stack sections vertically, reduce padding
- **Mobile (<768px)**: Full-width stacked layout, bottom nav, smaller typography scale
- All fixed heights removed — use `min-height: 100vh` or content-driven height

## Files to Modify

### Templates
1. `src/main/resources/templates/home.html`
   - Add `lang="ko"` to `<html>`
   - Remove inline styles from login/logout buttons
   - Add semantic `<nav>` elements with aria labels
   - Add meaningful `alt` text to all images
   - Add `<footer>` element
   - Fix typo: `card-buttom` → `card-bottom`, `community-buttom` → `community-button`
   - Restructure hero section with cleaner markup

### CSS
2. `src/main/resources/static/css/home.css`
   - Remove global `* { color, font-family }` overrides
   - Use CSS variables from `reset.css` consistently
   - Replace all fixed heights with content-driven sizing
   - Remove duplicate selectors (`.card-top`, `.fortune-list>li`)
   - Clean up `!important` usage in media queries
   - Fix `.community-box2` selector mismatch with HTML
   - Restructure card styles to match new design
   - Remove `.spline-logo-overlay` styles (use clean transition)
   - Add footer styles
   - Remove `.today-buttom` if unused (typo)

### New CSS Variables (add to reset.css)
3. `src/main/resources/static/css/reset.css`
   - Add `--deep`, `--dark`, `--mid`, `--light`, `--text`, `--accent` token variables

## Design Principles Applied
1. **No AI slop**: no purple-blue gradients, glass effects, over-rounded cards, decorative blobs, or floating shadows
2. **Typography-first**: content hierarchy driven by type size, weight, and letter-spacing
3. **Color discipline**: token-only colors, no arbitrary hex values
4. **Restraint**: minimal decorative elements, intentional whitespace
5. **Consistency**: uniform card styling across fortune and community sections
