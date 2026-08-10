export const BREAKPOINTS = {
  xs: 480,
  sm: 768,
  md: 1024,
  lg: 1280,
} as const;

export type Breakpoint = keyof typeof BREAKPOINTS;
