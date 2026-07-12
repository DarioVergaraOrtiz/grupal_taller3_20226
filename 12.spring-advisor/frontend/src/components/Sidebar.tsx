import React from 'react';
import { Session } from '../types/chat';
import logoComputacion from '../assets/logoComputacion.png';
import grupo1Avatar from '../assets/grupo1-avatar.jpg';
import girlAvatar from '../assets/girl_avatar.jpg';
import boyAvatar from '../assets/boy_avatar.jpg';

interface SidebarProps {
  sessions: Session[];
  currentSessionId: string | null;
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  onSelectSession: (id: string) => void;
  onNewChat: () => void;
  onDeleteSession: (id: string, e: React.MouseEvent) => void;
  onSearchChange: (query: string) => void;
  searchQuery: string;
  isOffline: boolean;
  onReconnect: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  sessions,
  currentSessionId,
  isCollapsed,
  onToggleCollapse,
  onSelectSession,
  onNewChat,
  onDeleteSession,
  onSearchChange,
  searchQuery,
  isOffline,
  onReconnect
}) => {
  return (
    <aside className={`sidebar ${isCollapsed ? 'collapsed' : 'open'}`} id="sidebar">
      {/* Sidebar Header */}
      <div className="sidebar-header">
        <button className="btn-icon btn-collapse" onClick={onToggleCollapse} title={isCollapsed ? "Expandir menú" : "Contraer menú"}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>
        
        {!isCollapsed && (
          <div className="uce-logo-container">
            <img src={logoComputacion} alt="Computación UCE" className="uce-sidebar-logo" />
            <div className="uce-logo-text-wrapper">
              <span className="uce-logo-title">Computación</span>
              <span className="uce-logo-subtitle">FICA - UCE</span>
            </div>
          </div>
        )}
      </div>

      {/* New Chat Button */}
      <button className="btn-new-chat-gemini" onClick={onNewChat} title="Nueva conversación">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
          <path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
        </svg>
        {!isCollapsed && <span>Nueva conversación</span>}
      </button>

      {/* Search Bar */}
      {!isCollapsed && (
        <div className="sidebar-search-container">
          <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            type="text"
            placeholder="Buscar conversaciones"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
      )}

      {/* Navigation List */}
      <div className="sidebar-scrollable-content">

        {/* Sessions Section */}
        <div className="sidebar-section">
          {!isCollapsed && <h3 className="sidebar-section-title">Recientes</h3>}
          <div className="sessions-list">
            {sessions.map((session) => (
              <button
                key={session.id}
                className={`session-item-gemini ${session.id === currentSessionId ? 'active' : ''}`}
                onClick={() => onSelectSession(session.id)}
                title={session.name}
              >
                <svg className="chat-bubble-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                </svg>
                
                {!isCollapsed && <span className="session-label">{session.name}</span>}
                
                {!isCollapsed && (
                  <span className="session-delete-btn" onClick={(e) => onDeleteSession(session.id, e)} title="Eliminar chat">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Offline Warning */}
        {isOffline && (
          <div className={`offline-status ${isCollapsed ? 'collapsed' : ''}`}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M1 1l22 22M16.72 11.06A10.94 10.94 0 0 1 19 12.55M5 12.55a10.94 10.94 0 0 1 5.17-2.39M10.71 5.05A16 16 0 0 1 22.58 9M1.42 9a15.91 15.91 0 0 1 12.21-4.07M8.53 16.11a6 6 0 0 1 6.95 0M12 20h.01"></path>
            </svg>
            {!isCollapsed && (
              <div className="offline-text">
                <span>Sin conexión</span>
                <button onClick={onReconnect}>Volver a cargar</button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Sidebar Footer */}
      <div className="sidebar-footer-gemini">
        <div className="profile-container" title="Grupo 1">
          <div className="sidebar-avatars-group">
            <img src={girlAvatar} alt="Niña" className="sidebar-avatar-img" />
            <img src={grupo1Avatar} alt="Grupo 1" className="sidebar-avatar-img main-avatar" />
            <img src={boyAvatar} alt="Niño" className="sidebar-avatar-img" />
          </div>
          {!isCollapsed && (
            <div className="profile-info">
              <span className="profile-name">Grupo 1</span>
              <span className="profile-badge">FICA</span>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};
