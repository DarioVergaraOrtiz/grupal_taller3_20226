import React, { useState, useRef } from 'react';
import { Message } from '../types/chat';
import logoComputacion from '../assets/logoComputacion.png';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import ReactMarkdown from 'react-markdown';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';

gsap.registerPlugin(useGSAP);

interface MessageItemProps {
  message: Message;
  isGenerating: boolean;
}

export const MessageItem: React.FC<MessageItemProps> = ({ message, isGenerating }) => {
  const { role, content } = message;
  const [copied, setCopied] = useState(false);
  const [liked, setLiked] = useState<boolean | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useGSAP(() => {
    // Animación de entrada para el mensaje
    gsap.from(containerRef.current, {
      y: 20,
      opacity: 0,
      duration: 0.5,
      ease: 'power3.out',
    });
  }, { scope: containerRef });

  const handleCopy = async () => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(content);
      } else {
        const textArea = document.createElement("textarea");
        textArea.value = content;
        textArea.style.position = "fixed";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
      }
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.warn("Fallo al copiar texto:", err);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };



  return (
    <div ref={containerRef} className={`message-item-gemini ${role}`}>
      {role === 'assistant' && (
        <div className="message-avatar-uce">
          <img 
            src={logoComputacion} 
            alt="Asistente UCE" 
            className={`uce-assistant-avatar-img ${isGenerating && !content ? 'pulse-avatar' : ''}`} 
          />
        </div>
      )}

      <div className="message-bubble-wrapper">
        <div className="message-bubble-gemini">
          {role === 'assistant' ? (
            content ? (
              <div className={`message-markdown ${isGenerating ? 'generating-cursor' : ''}`}>
                <ReactMarkdown
                  remarkPlugins={[remarkMath]}
                  rehypePlugins={[rehypeKatex]}
                >
                  {content}
                </ReactMarkdown>
              </div>
            ) : (
              <div className="gemini-typing-loader">
                <span></span>
                <span></span>
                <span></span>
              </div>
            )
          ) : (
            <div className="message-text-user">{content}</div>
          )}
        </div>

        {role === 'assistant' && content && (
          <div className="message-actions-gemini">
            {/* Copy Button */}
            <button 
              className={`action-btn ${copied ? 'active' : ''}`} 
              onClick={handleCopy}
              title={copied ? "¡Copiado!" : "Copiar respuesta"}
            >
              {copied ? (
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              ) : (
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                </svg>
              )}
              {copied && <span className="toast-tooltip">Copiado</span>}
            </button>

            {/* Like/Thumbs up Button */}
            <button 
              className={`action-btn ${liked === true ? 'liked' : ''}`} 
              onClick={() => setLiked(liked === true ? null : true)}
              title="Buen resultado"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>
              </svg>
            </button>

            {/* Dislike/Thumbs down Button */}
            <button 
              className={`action-btn ${liked === false ? 'disliked' : ''}`} 
              onClick={() => setLiked(liked === false ? null : false)}
              title="Mal resultado"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3zm7-13h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3"></path>
              </svg>
            </button>

            {/* Metrics */}
            {message.tokens !== undefined && message.timeMs !== undefined && (
              <div className="message-metrics-gemini">
                <div className="metric-item" title="Tokens consumidos">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                    <polyline points="2 17 12 22 22 17"></polyline>
                    <polyline points="2 12 12 17 22 12"></polyline>
                  </svg>
                  <span>{message.tokens} tokens</span>
                </div>
                <div className="metric-item" title="Tiempo de respuesta">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 16 14"></polyline>
                  </svg>
                  <span>{(message.timeMs / 1000).toFixed(1)}s</span>
                </div>
                <div className="metric-item" title="Velocidad de generación">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 14l4-4"></path>
                    <path d="M3.34 16A10 10 0 1 1 20.66 16"></path>
                  </svg>
                  <span>{((message.tokens) / (message.timeMs / 1000)).toFixed(2)} t/s</span>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
