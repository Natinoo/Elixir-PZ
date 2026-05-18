import { useState, useEffect } from 'react';

export function useCountdown(overlimitSince) {
  const [remaining, setRemaining] = useState(null);

  useEffect(() => {
    if (!overlimitSince) { setRemaining(null); return; }

    const calc = () => {
      const deadline = new Date(overlimitSince).getTime() + 2 * 60 * 60 * 1000;
      const diff = deadline - Date.now();
      setRemaining(diff > 0 ? diff : 0);
    };

    calc();
    const interval = setInterval(calc, 1000);
    return () => clearInterval(interval);
  }, [overlimitSince]);

  return remaining;
}