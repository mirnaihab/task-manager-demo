---
name: frontend-patterns
description: this skill is for frontend architecture decisions and implementation patterns across modern web projects.
---

### Aesthetic & Responsive UI/UX Design Guidelines

This document outlines the core principles and actionable decisions for creating beautiful, responsive, and user-centric applications.

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
2. if you will establish a new design, check the DESIGN.md files in this github repo: https://github.com/VoltAgent/awesome-design-md by using `#fetch` for inspiration and best practices based on user intent


## Non-negotiables 
- you must always use TypeScript for new frontend code unless the user explicitly asks otherwise or the project does not already use it.
- Use Tailwind CSS v4 or later for most frontend styling.
- Use Material UI with styled components for heavily customized design-system-heavy applications.
- Use TanStack Query for API-heavy apps with complex server-state coordination.
- prefer to use hooks over logic inside components, and separate data fetching and business logic into custom hooks or services.
- Always validate external data with Zod schemas or similar validation libraries.

## Architecture selection defaults
- Default web stack: Next.js. 15+ for server components, app router, and React 18+ features.
- Fast SPA/internal tool option: React 19+. see react patterns below for more details.
- Use React Hook Form for form handling.
- Use charting libraries such as React Charts when visualization is a core requirement.
- Avoid excessive `useEffect` usage and prefer declarative data and state flows.
- Use `useMemo` and `useCallback` to optimize expensive computations and prevent unnecessary re-renders.
- Use `React.Suspense` and `React.lazy` for code-splitting and lazy loading components.

## Project structure guidance

- `pages` hold route-level composition.
- `layouts` organize page-level structure.
- `components` stay reusable and focused.
- `services` and custom hooks hold data and business logic.
- `styles` contain Tailwind config and global styles.


# Default component and fetching patterns

## Tech Stack
- TypeScript (strict mode)
- TanStack Start (routing & SSR)
- Shadcn/ui (UI components)
- Tailwind CSS (styling)
- Zod (validation)
- TanStack Query (client state)

## Code Style Rules

- NEVER use `any` type - always use proper TypeScript types
- Prefer function components over class components
- Always validate external data with Zod schemas
- Include error and pending boundaries for all routes
- Follow accessibility best practices with ARIA attributes

## Component Patterns

Use function components with proper TypeScript interfaces:

```typescript
interface ButtonProps {
  children: React.ReactNode;
  onClick: () => void;
  variant?: 'primary' | 'secondary';
}

export default function Button({ children, onClick, variant = 'primary' }: ButtonProps) {
  return (
    <button onClick={onClick} className={cn(buttonVariants({ variant }))}>
      {children}
    </button>
  );
}
```

## Data Fetching

Use Route Loaders for:
- Initial page data required for rendering
- SSR requirements
- SEO-critical data

Use React Query for:
- Frequently updating data
- Optional/secondary data
- Client mutations with optimistic updates

```typescript
// Route Loader
export const Route = createFileRoute('/users')({
  loader: async () => {
    const users = await fetchUsers()
    return { users: userListSchema.parse(users) }
  },
  component: UserList,
})

// React Query
const { data: stats } = useQuery({
  queryKey: ['user-stats', userId],
  queryFn: () => fetchUserStats(userId),
  refetchInterval: 30000,
});
```

## Zod Validation

Always validate external data. Define schemas in `src/lib/schemas.ts`:

```typescript
export const userSchema = z.object({
  id: z.string(),
  name: z.string().min(1).max(100),
  email: z.string().email().optional(),
  role: z.enum(['admin', 'user']).default('user'),
})

export type User = z.infer<typeof userSchema>

// Safe parsing
const result = userSchema.safeParse(data)
if (!result.success) {
  console.error('Validation failed:', result.error.format())
  return null
}
```

## Routes

Structure routes in `src/routes/` with file-based routing. Always include error and pending boundaries:

```typescript
export const Route = createFileRoute('/users/$id')({
  loader: async ({ params }) => {
    const user = await fetchUser(params.id);
    return { user: userSchema.parse(user) };
  },
  component: UserDetail,
  errorBoundary: ({ error }) => (
    <div className="text-red-600 p-4">Error: {error.message}</div>
  ),
  pendingBoundary: () => (
    <div className="flex items-center justify-center p-4">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
    </div>
  ),
});
```

## UI Components

Always prefer Shadcn/ui components over custom ones:

```typescript
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

<Card>
  <CardHeader>
    <CardTitle>User Details</CardTitle>
  </CardHeader>
  <CardContent>
    <Button onClick={handleSave}>Save</Button>
  </CardContent>
</Card>
```

Use Tailwind for styling with responsive design:

```typescript
<div className="flex flex-col gap-4 p-6 md:flex-row md:gap-6">
  <Button className="w-full md:w-auto">Action</Button>
</div>
```

## Accessibility

Use semantic HTML first. Only add ARIA when no semantic equivalent exists:

```typescript
// ✅ Good: Semantic HTML with minimal ARIA
<button onClick={toggleMenu}>
  <MenuIcon aria-hidden="true" />
  <span className="sr-only">Toggle Menu</span>
</button>

// ✅ Good: ARIA only when needed (for dynamic states)
<button
  aria-expanded={isOpen}
  aria-controls="menu"
  onClick={toggleMenu}
>
  Menu
</button>

// ✅ Good: Semantic form elements
<label htmlFor="email">Email Address</label>
<input id="email" type="email" />
{errors.email && (
  <p role="alert">{errors.email}</p>
)}
```

## File Organization

```
src/
├── components/ui/    # Shadcn/ui components
├── lib/schemas.ts    # Zod schemas
├── routes/          # File-based routes
└── routes/api/      # Server routes (.ts)
```
