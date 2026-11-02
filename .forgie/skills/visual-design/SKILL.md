---
name: visual-design
description: Master visual design skill covering color theory, typography, layout, and visual hierarchy. Use this when designing user interfaces, creating visual assets, or reviewing design work to ensure aesthetic appeal and usability. Use this to create the DESIGN.md file for any design system, or any visual output. Typically used with the "frontend-slides" skill to create visually appealing slide decks, or with the "high-level-design" skill to create visually appealing design systems for software applications, or with "frontend-patterns" skill to create visually appealing frontend patterns.
---

#### 1. Core Philosophy: Clarity, Consistency, Simplicity

Before any specific design choice, adhere to these principles:
*   **Clarity:** The user should always understand what they are seeing and what will happen when they take an action. Avoid ambiguity.
*   **Consistency:** Elements that look the same should behave the same. A consistent design language reduces the cognitive load on the user, making the app feel intuitive.
*   **Simplicity:** Less is more. Every element on the screen should serve a purpose. Aggressively remove clutter to focus the user's attention on what matters.

#### 2. The Foundational Pillars of Visual Design

These three areas form the bedrock of your application's look and feel.

**A. Layout & Spacing: Creating Rhythm and Hierarchy**

A structured layout is the skeleton of a beautiful app.

*   **Use a Grid System:** All layouts should be built on a grid (e.g., a 12-column grid is a web standard). This ensures alignment and a professional, organized look.
*   **Establish a Spacing Scale:** Do not use random margin or padding values. Use a consistent scale based on a multiple of 4 or 8 (e.g., 4px, 8px, 12px, 16px, 24px, 32px). This creates a visual rhythm and makes components feel like they belong together.
    *   **Decision:** Use an 8-point grid system for all spacing and sizing.
*   **Embrace Whitespace:** Whitespace (or negative space) is not empty space; it's a powerful design tool. Use it generously to group related items, separate unrelated ones, and give your content room to breathe.

**B. Typography: The Voice of Your Application**

Typography is 90% of design. Getting it right is critical for usability and aesthetics.

*   **Limit Font Families:** Do not use more than two fonts in your application—one for headings (a "display" font) and one for body text (a "body" font). This ensures consistency and avoids a chaotic look.
    *   **Decision:** Use a clean, sans-serif font like **Inter**, **Manrope**, or the system UI font stack for maximum readability.
*   **Establish a Type Scale:** Just like with spacing, define a clear hierarchy for text sizes (e.g., 12px, 14px, 16px, 20px, 24px, 32px). This visually communicates the importance of different pieces of information.
*   **Prioritize Readability:**
    *   **Line Height:** Set body text line-height to ~1.5x the font size for comfortable reading.
    *   **Line Length:** Aim for 50-75 characters per line. Lines that are too long or too short are fatiguing to read.

**C. Color Palette: Setting the Mood and Guiding the Eye**

Color is used to create hierarchy, convey meaning, and establish a brand.

*   **Define a Structured Palette:**
    *   **Primary (1-2 colors):** Your main brand colors. Used for primary actions and key elements.
    *   **Secondary (1-2 colors):** Complements the primary colors. Used for secondary actions and highlighting information.
    *   **Neutrals (3-5 shades):** Your grays and off-whites. Used for text, backgrounds, and borders. A good range of neutrals is essential for a clean UI.
    *   **Semantic Colors (4 colors):** Colors with specific meanings:
        *   **Success (Green):** For confirmations and positive feedback.
        *   **Warning (Yellow/Orange):** For potential issues or alerts.
        *   **Error (Red):** For validation errors and critical failures.
        *   **Info (Blue):** For informational messages.
*   **Check for Accessibility:** Ensure all text has a sufficient contrast ratio against its background to be readable by users with visual impairments. Use a WCAG contrast checker tool.
    *   **Decision:** All text/background color combinations **must** pass WCAG AA standards.

#### 3. Component & Interaction Design

*   **Design for States:** A component is never static. Explicitly design for all its states: default, hover, focused, active, disabled, loading, and empty. This makes the UI feel responsive and alive.
*   **Provide Instant Feedback:** When a user performs an action, the UI must provide immediate feedback. This can be a spinner on a button, a success toast, or a validation message. Never leave the user wondering if their action was registered.

#### 4. Responsiveness & Adaptability

Your application must be usable and beautiful on any screen size.

*   **Adopt a Mobile-First Approach:** Design for the smallest screen (mobile) first. This forces you to prioritize the most important content and features. Then, use media queries to "progressively enhance" the layout for larger screens.
*   **Use Fluid Layouts:** Use relative units like percentages (%) and viewport units (vw, vh) for containers, so they adapt smoothly to different screen sizes.
*   **Define Breakpoints Logically:** Don't define breakpoints based on specific devices (e.g., "iPhone" or "iPad"). Define them where your layout naturally "breaks" or starts to look awkward. Common breakpoints are `sm`, `md`, `lg`, `xl`.
*   **Optimize Touch Targets:** On mobile, ensure all interactive elements (buttons, links) have a large enough touch target (at least 44x44px) to be easily tappable.
  
#### 5. Cognitive load
less is more, be mindful of negative space and number of clicks the user needs to make before they get somewhere "important". information should be clear and simple to understand especially if it is important information.

# Game plan
1. Stick to existing project design patterns or design system and styles for consistency. If none exist, establish a new design language based on the guidelines above.
2. if you will establish a new design, check the DESIGN.md files in this github repo: https://github.com/VoltAgent/awesome-design-md for inspiration and best practices based on user intent

# Instructions for Visual Design
- **always** prioritize clarity and usability over aesthetics
- **always** fill a DESIGN.md file with the design system for the requested artifact, including color palette, typography, spacing, and component styles. The Template for the DESIGN.md file is provided below.
- **prioritize** simple design frameworks over complex ones, shadcn + tailwindcss is a good example of a simple design framework 
- **always** use the "Ai look and feel" as a reference for the design system, but avoid generic, template-like designs that lack personality or uniqueness. Avoid using m-dashes, cards with side bars, or other common design patterns that make the design feel generic or uninspired.
- If you think it would be useful, use web search to find design inspiration, but avoid copying existing designs. Instead, use them as a reference to create something unique and tailored to the user's needs instead of producing the first design that comes to mind.

## "Ai look and feel" to avoid:
- **Avoid** generic, template-like designs that lack personality or uniqueness.
- **Avoid** using em-dashes, cards with side bars, or other common design patterns that make the design feel generic or uninspired.

# Template for DESIGN.md file

```markdown
```yaml
name: "[System Name]"
description: "[Brief 1-sentence description of the design system]"

colors:
  primary: "[Hex Code]"
  secondary: "[Hex Code]"
  surface: "[Hex Code, e.g., page backgrounds]"
  on-surface: "[Hex Code, e.g., primary text on surface]"
  surface-variant: "[Hex Code, e.g., card backgrounds]"
  on-surface-variant: "[Hex Code]"
  outline: "[Hex Code, e.g., borders and dividers]"
  error: "[Hex Code]"

typography:
  headline-lg:
    fontFamily: [Font Family, e.g., Inter]
    fontSize: [Size, e.g., 32px]
    fontWeight: [Weight, e.g., 700]
  title-md:
    fontFamily: [Font Family]
    fontSize: [Size, e.g., 16px]
    fontWeight: [Weight, e.g., 600]
  body-md:
    fontFamily: [Font Family]
    fontSize: [Size, e.g., 14px]
    fontWeight: [Weight, e.g., 400]
  label-sm:
    fontFamily: [Font Family]
    fontSize: [Size, e.g., 12px]
    fontWeight: [Weight, e.g., 500]

rounded:
  none: 0px
  sm: 4px
  md: 8px
  lg: 16px
  full: 9999px

spacing:
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
```

# Design System: [System Name]

## 1. Visual Theme & Atmosphere
[Describe the mood, visual density, and overarching design philosophy. e.g., "Minimalist and airy", "Data-heavy and analytical", "Playful and approachable".]

## 2. Color Palette & Roles
- **Primary:** `[Hex Code]` - [When and where to use the primary color]
- **Secondary:** `[Hex Code]` - [Usage guidelines]
- **Surface:** `[Hex Code]` - [Main background usage]
- **Surface Variant:** `[Hex Code]` - [Cards, sub-sections, muted backgrounds]
- **Outline:** `[Hex Code]` - [Borders, dividers, strokes]
- **Error:** `[Hex Code]` - [Destructive actions, error text]
- colour usage instructions: 
  - <primary> is used for primary actions, highlights, and key elements.
  - <secondary> is used for secondary actions, accents, and less prominent elements.
  - <surface> is used for main backgrounds.
  - <surface-variant> is used for cards, sub-sections, and muted backgrounds.
  - <outline> is used for borders, dividers, and strokes.
  - <error> is used for destructive actions and error text.
  some examples: `<Card background="<surface>" border="<outline>"> <Text color="<on-surface>">Card Content</Text> </Card>`

## 3. Typography Rules
- **Headline (LG):** `var(--text-headline-lg)` - [Usage]
- **Title (MD):** `var(--text-title-md)` - [Usage]
- **Body (MD):** `var(--text-body-md)` - [Usage]
- **Label (SM):** `var(--text-label-sm)` - [Usage]

## 4. Component Stylings
- **Buttons (Primary):** [Styles, radius, padding, active/hover states]
- **Buttons (Secondary):** [Styles, radius, padding]
- **Inputs:** [Border styles, focus outlines, validation styling]
- **Chips/Tags:** [Border radius, text size, padding]

## 5. Layout Principles
- **Grid System:** [e.g., "Use a 12-column grid with a `spacing-lg` gutter."]
- **Whitespace:** [e.g., "Favor generous whitespace between sections (`spacing-xl`)."]

## 6. Depth & Elevation
- **Cards:** [Shadow vs flat, border treatments. e.g., "Cards are flat with a 1px outline."]
- **Modals/Popovers:** [Drop shadows, scrims, overlay colors]
- **Navigation:** [Sticky headers, drop shadows on scroll]

## 7. Do's and Don'ts
- **Do:** [Specific design rule]
- **Do:** [Specific design rule]
- **Don't:** [Specific anti-pattern] for example: what does "bland"/"ugly" mean in this project?
- **Don't:** [Specific anti-pattern]

## 8. Responsive Behavior
- **Mobile (<768px):** [How components stack, margin adjustments]
- **Tablet (768px - 1024px):** [Grid shifting, sidebar behavior]
- **Desktop (>1024px):** [Max-width containers, multi-column setups]

## 9. Interactivity
- **Hover States:** [Color changes, shadows, cursor changes]
- **Focus States:** [Outline styles, color changes]
- **Active States:** [Pressed button styles, loading indicators]
- **Animations:** [Subtle transitions, duration, easing]

## 10. Screens design system
- **Login Screen:** [Layout, spacing, component usage]
- **Dashboard:** [Layout, spacing, component usage]
- **Settings Page:** [Layout, spacing, component usage]
- **Home Page:** [Layout, spacing, component usage]
- <add other screens as needed>
```