import React, { useRef, useEffect } from 'react';
import { Message } from '../types/chat';
import { MessageItem } from './MessageItem';
import logoComputacion from '../assets/logoComputacion.png';
import sellosUce from '../assets/sellos-uce.png';

interface ChatAreaProps {
  messages: Message[];
  isGenerating: boolean;
  onCardClick: (prompt: string) => void;
}

export const ChatArea: React.FC<ChatAreaProps> = ({ messages, isGenerating, onCardClick }) => {
  const chatBottomRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom of chat
  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isGenerating]);

  const welcomeCards = [
    {
      prompt: '¿Cuáles son los requisitos para titulación?',
      title: 'Requisitos de titulación',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M22 10v6M2 10l10-5 10 5-10 5z"></path>
          <path d="M6 12v5c3 3 9 3 12 0v-5"></path>
        </svg>
      )
    },
    {
      prompt: '¿Qué es el proyecto integrador y cuál es su formato?',
      title: 'Proyecto integrador',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
        </svg>
      )
    },
    {
      prompt: '¿Cuáles son los plazos importantes para el proceso de titulación?',
      title: 'Plazos y fechas clave',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
      )
    }
  ];

  return (
    <div className="chat-area-gemini" id="chat-area">
      {/* Background Radial Glow */}
      <div className="radial-glow-overlay"></div>

      <div className="chat-content-container">
        {messages.length === 0 ? (
          /* Welcome Screen */
          <div className="welcome-screen-gemini">
            <div className="welcome-header-logos">
              <img src={sellosUce} alt="Sello UCE" className="welcome-logo-sello" />
              <img src={logoComputacion} alt="Logo Computación" className="welcome-logo-computacion" />
            </div>
            <h2 className="welcome-title">Asistente Virtual de Computación</h2>
            <p className="welcome-subtitle-desc">
              Desarrollado por la <strong>Carrera de Computación</strong> de la Facultad de Ingeniería y Ciencias Aplicadas (FICA - UCE). 
              Pregúntame sobre el proceso de titulación, proyectos integradores y normativas académicas.
            </p>
            
            <div className="welcome-cards-gemini">
              {welcomeCards.map((card, index) => (
                <button 
                  key={index} 
                  className="welcome-card-gemini" 
                  onClick={() => onCardClick(card.prompt)}
                >
                  <div className="card-icon">{card.icon}</div>
                  <span>{card.title}</span>
                </button>
              ))}
            </div>
          </div>
        ) : (
          /* Messages List */
          <div className="messages-list-gemini">
            {messages.map((msg, index) => {
              const isLastAssistantMessage = 
                msg.role === 'assistant' && 
                index === messages.length - 1;
              return (
                <MessageItem 
                  key={index} 
                  message={msg} 
                  isGenerating={isLastAssistantMessage && isGenerating}
                />
              );
            })}
            <div ref={chatBottomRef} />
          </div>
        )}
      </div>
    </div>
  );
};
