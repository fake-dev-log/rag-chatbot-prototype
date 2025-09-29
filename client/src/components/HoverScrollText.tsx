import { useLayoutEffect, useRef, useState } from 'react';

interface HoverScrollTextProps {
  text: string;
  className?: string;
}

function HoverScrollText({ text, className }: HoverScrollTextProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [isOverflowing, setIsOverflowing] = useState(false);

  useLayoutEffect(() => {
    const container = containerRef.current;
    if (container) {
      // Check if the scrollWidth (total content width) is greater than the clientWidth (visible width)
      const checkOverflow = container.scrollWidth > container.clientWidth;
      setIsOverflowing(checkOverflow);
    }
  }, [text]); // Rerun when text changes

  return (
    <div
      ref={containerRef}
      className={`hover-scroll-container ${isOverflowing ? 'can-scroll' : ''}`}
    >
      <span className={`hover-scroll-text ${className}`}>
        {text}
      </span>
    </div>
  );
}

export default HoverScrollText;
