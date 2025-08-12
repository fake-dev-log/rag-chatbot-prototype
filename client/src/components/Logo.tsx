export default function Logo({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="32" cy="32" r="30" stroke="#4F46E5" strokeWidth="4" />
      <path d="M20 32L28 40L44 24" stroke="#4F46E5" strokeWidth="4" strokeLinecap="round" />
    </svg>
  );
}