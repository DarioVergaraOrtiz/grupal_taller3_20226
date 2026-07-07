import React, { useState } from 'react';
import { Message } from '../types/chat';

interface MessageItemProps {
  message: Message;
  isGenerating: boolean;
}

export const MessageItem: React.FC<MessageItemProps> = ({ message, isGenerating }) => {
  const { role, content } = message;
  const [copied, setCopied] = useState(false);
  const [liked, setLiked] = useState<boolean | null>(null);

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

  const parseMarkdown = (text: string) => {
    if (!text) return '';
    try {
      if ((window as any).marked) {
        return (window as any).marked.parse(text);
      }
    } catch (e) {
      console.error("Error parsing markdown with marked:", e);
    }
    // Fallback if marked is not available or fails
    return text.replace(/\n/g, '<br />');
  };

  return (
    <div className={`message-item-gemini ${role}`}>
      {role === 'assistant' && (
        <div className="message-avatar-gemini">
          <svg className={`gemini-star-icon ${isGenerating && !content ? 'spinning' : ''}`} width="28" height="28" viewBox="0 0 24 24" fill="none">
            <path d="M12 0L14.7 9.3L24 12L14.7 14.7L12 24L9.3 14.7L0 12L9.3 9.3L12 0Z" fill="url(#starGradient)"></path>
            <defs>
              <linearGradient id="starGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#4285F4"></stop>
                <stop offset="30%" stopColor="#9B72CB"></stop>
                <stop offset="70%" stopColor="#D96570"></stop>
                <stop offset="100%" stopColor="#F3AF3D"></stop>
              </linearGradient>
            </defs>
          </svg>
        </div>
      )}

      <div className="message-bubble-wrapper">
        <div className="message-bubble-gemini">
          {role === 'assistant' ? (
            content ? (
              <div 
                className="message-markdown"
                dangerouslySetInnerHTML={{ __html: parseMarkdown(content) }}
              />
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

            {/* Share Button */}
            <button className="action-btn" title="Compartir">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="18" cy="5" r="3"></circle>
                <circle cx="6" cy="12" r="3"></circle>
                <circle cx="18" cy="19" r="3"></circle>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
              </svg>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
