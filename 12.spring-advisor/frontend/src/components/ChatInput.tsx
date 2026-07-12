import React, { useState, useRef, useEffect } from 'react';
import 'regenerator-runtime/runtime';
import SpeechRecognition, { useSpeechRecognition } from 'react-speech-recognition';

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

  const {
    transcript,
    listening,
    resetTranscript,
    browserSupportsSpeechRecognition
  } = useSpeechRecognition();

  const handleMicClick = () => {
    if (!browserSupportsSpeechRecognition) {
      alert("Tu navegador no soporta dictado por voz nativamente. Por favor, usa Chrome o Safari 14.5+.");
      return;
    }
    
    if (listening) {
      SpeechRecognition.stopListening();
    } else {
      const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
      SpeechRecognition.startListening({ language: 'es-ES', continuous: !isIOS });
    }
  };

  useEffect(() => {
    if (!listening && transcript) {
      setText((prev) => prev + (prev ? ' ' : '') + transcript);
      resetTranscript();
    }
  }, [listening, transcript, resetTranscript]);

  const displayedText = text + (listening && transcript ? (text ? ' ' : '') + transcript : '');

  // Auto-resize textarea
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
    }
  }, [displayedText]);

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
    if (listening) {
      SpeechRecognition.stopListening();
    }
    const finalToSend = displayedText.trim();
    if (finalToSend && !isLoading) {
      onSendMessage(finalToSend);
      setText('');
      resetTranscript();
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

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setText(e.target.value);
    if (listening) {
      SpeechRecognition.stopListening();
    }
  };

  return (
    <div className="chat-input-container-gemini">
      <div className={`input-pill-gemini ${displayedText.trim() || listening ? 'typing' : ''}`}>
        {/* Text Input area */}
        <textarea
          ref={textareaRef}
          value={displayedText}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder={listening ? "Escuchando..." : "Pregunta a Gemini UCE"}
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
              <span>{selectedModel === 'pro' ? 'Con QDRANT' : 'Sin QDRANT'}</span>
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
                    <span className="model-name">gemini-3.1-flash-lite con Qdrant</span>
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
                    <span className="model-name">gemini-3.1-flash-lite sin Qdrant</span>
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

          {/* Mic Button */}
          <button 
            className={`pill-action-btn btn-mic ${listening ? 'recording' : ''}`} 
            onClick={handleMicClick}
            disabled={isLoading}
            title={listening ? "Grabando..." : "Escribir por voz"}
            style={{ color: listening ? '#ea4335' : 'var(--text-secondary)' }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
              <line x1="12" y1="19" x2="12" y2="23"></line>
              <line x1="8" y1="23" x2="16" y2="23"></line>
            </svg>
          </button>

          {/* Send Button */}
          <button 
            className={`pill-action-btn btn-send-gemini ${isLoading ? 'loading' : ''} ${!displayedText.trim() ? 'disabled' : ''}`} 
            onClick={handleSend}
            disabled={isLoading || !displayedText.trim()}
            title="Enviar mensaje"
            style={{ opacity: displayedText.trim() ? 1 : 0.5, cursor: displayedText.trim() ? 'pointer' : 'not-allowed' }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </div>
      </div>
      <div className="input-disclaimer">
        Gemini UCE puede mostrar información imprecisa, valida las fechas y reglamentos oficiales.
      </div>
    </div>
  );
};
