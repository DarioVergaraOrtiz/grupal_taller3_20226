import React, { useEffect, useRef } from 'react';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  color: string;
  rotation: number;
  rotationSpeed: number;
  type: number;
  depth: number;
  alpha: number;
  sprite: HTMLCanvasElement;
}

const colors = [
  '#4285F4', // Google Blue
  '#A855F7', // Google Purple
  '#EC4899', // Google Pink
  '#EA4335', // Google Red
  '#F97316', // Google Orange
  '#FBBC05', // Google Yellow
  '#34A853'  // Google Green
];

const interactionRadius = 150;
const friction = 0.96;
const gravity = -0.04; // Negative value to make particles float upwards

// Offscreen canvas cache to pre-render shapes (sprites) and optimize performance
const spriteCache: { [key: string]: HTMLCanvasElement } = {};

function getSprite(color: string, shapeType: number): HTMLCanvasElement {
  const key = `${color}-${shapeType}`;
  if (spriteCache[key]) return spriteCache[key];

  const size = 64;
  const center = size / 2;
  const drawSize = 20; // Shape bounding box size inside sprite
  
  const c = document.createElement('canvas');
  c.width = size;
  c.height = size;
  const cx = c.getContext('2d');
  
  if (cx) {
    // Elegant glow/shadow matching shape color
    cx.shadowColor = color;
    cx.shadowBlur = 8;
    cx.shadowOffsetX = 0;
    cx.shadowOffsetY = 0;
    
    cx.fillStyle = color;
    cx.translate(center, center);
    cx.beginPath();

    if (shapeType === 0) {
      // Circle
      cx.arc(0, 0, drawSize / 2, 0, Math.PI * 2);
    } else if (shapeType === 1) {
      // Square
      cx.rect(-drawSize / 2, -drawSize / 2, drawSize, drawSize);
    } else if (shapeType === 2) {
      // Triangle
      cx.moveTo(0, -drawSize / 2);
      cx.lineTo(drawSize / 2, drawSize / 2);
      cx.lineTo(-drawSize / 2, drawSize / 2);
      cx.closePath();
    }
    cx.fill();
  }

  spriteCache[key] = c;
  return c;
}

export const InteractiveParticles: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d', { alpha: true });
    if (!ctx) return;

    let width = window.innerWidth;
    let height = window.innerHeight;
    let animationFrameId: number;

    // Set canvas size accounting for Device Pixel Ratio for crystal clear rendering
    const resize = () => {
      const dpr = window.devicePixelRatio || 1;
      width = window.innerWidth;
      height = window.innerHeight;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
      ctx.scale(dpr, dpr);
    };

    window.addEventListener('resize', resize);
    resize();

    const particles: Particle[] = [];
    const particleCount = 50;

    class ParticleImpl implements Particle {
      x!: number;
      y!: number;
      vx!: number;
      vy!: number;
      size!: number;
      color!: string;
      rotation!: number;
      rotationSpeed!: number;
      type!: number;
      depth!: number;
      alpha!: number;
      sprite!: HTMLCanvasElement;

      constructor() {
        this.init(true);
      }

      init(randomY = false) {
        this.x = Math.random() * width;
        // If initializing, scatter randomly across height. Otherwise spawn just below screen.
        this.y = randomY ? Math.random() * height : height + 50;
        this.size = Math.random() * 6 + 4; // Base physical shape dimension
        this.vx = (Math.random() - 0.5) * 0.6;
        this.vy = (Math.random() - 0.5) * 0.3 + gravity; // Slower initial upward float
        this.color = colors[Math.floor(Math.random() * colors.length)];
        this.rotation = Math.random() * Math.PI * 2;
        this.rotationSpeed = (Math.random() - 0.5) * 0.015;
        this.type = Math.floor(Math.random() * 3); // 0 = Circle, 1 = Square, 2 = Triangle
        this.sprite = getSprite(this.color, this.type);
        this.depth = Math.random() * 1.2 + 0.3; // 3D depth layer (0.3 to 1.5)
        this.alpha = Math.random() * 0.2 + 0.15; // Elegant translucent alpha (0.15 to 0.35)
      }

      update(mouseX: number, mouseY: number) {
        // Apply "antigravity" upward acceleration scaled by depth
        this.vy += gravity * 0.02 * this.depth;
        this.x += this.vx * this.depth;
        this.y += this.vy * this.depth;
        this.rotation += this.rotationSpeed;

        // Mouse interaction logic (Repulsion force)
        const dx = this.x - mouseX;
        const dy = this.y - mouseY;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < interactionRadius) {
          const force = (interactionRadius - dist) / interactionRadius;
          const angle = Math.atan2(dy, dx);
          const push = force * 3.5;
          this.vx += Math.cos(angle) * push;
          this.vy += Math.sin(angle) * push;
        }

        // Apply friction to slow down after repulsion
        this.vx *= friction;
        this.vy *= friction;

        // Wrap around horizontally if drifting off screen edges
        if (this.x < -50) this.x = width + 50;
        if (this.x > width + 50) this.x = -50;

        // Recycle particle once it floats past the top boundary
        if (this.y < -60) {
          this.init(false);
        }
      }

      draw() {
        ctx.save();
        ctx.translate(this.x, this.y);
        ctx.rotate(this.rotation);
        
        // Scale render dimensions according to size and depth layer
        const scaleFactor = (this.size * this.depth) / 20;
        const renderSize = 64 * scaleFactor;
        
        // Farthest particles are fainter to enhance the 3D perspective
        ctx.globalAlpha = this.alpha * (this.depth / 1.5);
        ctx.drawImage(this.sprite, -renderSize / 2, -renderSize / 2, renderSize, renderSize);
        ctx.restore();
      }
    }

    // Populate initial particle system
    for (let i = 0; i < particleCount; i++) {
      particles.push(new ParticleImpl());
    }

    // Mouse coordinates initialization (outside canvas viewport initially)
    let mouseX = -1000;
    let mouseY = -1000;

    const handleMouseMove = (e: MouseEvent) => {
      mouseX = e.clientX;
      mouseY = e.clientY;
    };

    const handleTouchMove = (e: TouchEvent) => {
      if (e.touches.length > 0) {
        mouseX = e.touches[0].clientX;
        mouseY = e.touches[0].clientY;
      }
    };

    const handleMouseLeave = () => {
      mouseX = -1000;
      mouseY = -1000;
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('touchmove', handleTouchMove);
    window.addEventListener('mouseleave', handleMouseLeave);

    const animate = () => {
      ctx.clearRect(0, 0, width, height);
      
      particles.forEach((p) => {
        const pImpl = p as ParticleImpl;
        pImpl.update(mouseX, mouseY);
        pImpl.draw();
      });

      animationFrameId = requestAnimationFrame(animate);
    };

    animate();

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', resize);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('mouseleave', handleMouseLeave);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      style={{
        display: 'block',
        position: 'fixed',
        top: 0,
        left: 0,
        zIndex: 2, // Placed behind chat text (zIndex: 5) but above radial glow overlay
        pointerEvents: 'none',
      }}
    />
  );
};

export default InteractiveParticles;
