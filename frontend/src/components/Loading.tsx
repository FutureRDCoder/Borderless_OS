export function Loading({ label = 'Loading…' }: { label?: string }) {
  return <p className="muted loading">{label}</p>
}
