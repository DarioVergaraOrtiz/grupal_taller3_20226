import React, { useState, useRef, useEffect } from 'react';

interface ChatInputProps {
  onSendMessage: (text: string) => void;
  isLoading: boolean;
  selectedModel: 'pro' | 'flash';
  onModelChange: (model: 'pro' | 'flash') => void;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSendMessage,
  isLoading,
  selectedModel,
  onModelChange
}) => {
  const [text, setText] = useState('');
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Auto-resize textarea
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
    }
  }, [text]);

  // Close dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSend = () => {
    if (text.trim() && !isLoading) {
      onSendMessage(text.trim());
      setText('');
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="chat-input-container-gemini">
      <div className="input-pill-gemini">
        {/* Attachment "+" Button */}
        <button className="pill-action-btn btn-attach" title="Añadir archivos">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </button>

        {/* Text Input area */}
        <textarea
          ref={textareaRef}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Pregunta a Gemini UCE"
          rows={1}
          disabled={isLoading}
        />

        {/* Model Selector Dropdown & Actions */}
        <div className="input-right-actions">
          {/* Model Selector */}
          <div className="model-selector-wrapper" ref={dropdownRef}>
            <button 
              className="model-select-pill" 
              onClick={() => setIsDropdownOpen(!isDropdownOpen)}
              title="Cambiar modelo"
            >
              <span>{selectedModel === 'pro' ? 'Pro' : 'Flash'}</span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </button>

            {isDropdownOpen && (
              <div className="model-dropdown-menu">
                <button 
                  className={`model-option ${selectedModel === 'pro' ? 'active' : ''}`}
                  onClick={() => {
                    onModelChange('pro');
                    setIsDropdownOpen(false);
                  }}
                >
                  <div className="model-option-info">
                    <span className="model-name">Gemini Pro</span>
                    <span className="model-desc">RAG local + Base de conocimiento UCE</span>
                  </div>
                  {selectedModel === 'pro' && (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                  )}
                </button>

                <button 
                  className={`model-option ${selectedModel === 'flash' ? 'active' : ''}`}
                  onClick={() => {
                    onModelChange('flash');
                    setIsDropdownOpen(false);
                  }}
                >
                  <div className="model-option-info">
                    <span className="model-name">Gemini Flash</span>
                    <span className="model-desc">Respuestas rápidas directas</span>
                  </div>
                  {selectedModel === 'flash' && (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                  )}
                </button>
              </div>
            )}
          </div>

          {/* Voice Input or Send Button */}
          {text.trim() ? (
            <button 
              className={`pill-action-btn btn-send-gemini ${isLoading ? 'loading' : ''}`} 
              onClick={handleSend}
              disabled={isLoading}
              title="Enviar mensaje"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            </button>
          ) : (
            <button className="pill-action-btn btn-mic" title="Usar micrófono (simulado)">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                <line x1="12" y1="19" x2="12" y2="23"></line>
                <line x1="8" y1="23" x2="16" y2="23"></line>
              </svg>
            </button>
          )}
        </div>
      </div>
      <div className="input-disclaimer">
        Gemini UCE puede mostrar información imprecisa, valida las fechas y reglamentos oficiales.
      </div>
    </div>
  );
};
