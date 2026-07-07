import React from 'react';
import { Session } from '../types/chat';

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
    <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''}`} id="sidebar">
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
          <div className="gemini-logo-container">
            <svg className="gemini-star-logo" width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M12 0L14.7 9.3L24 12L14.7 14.7L12 24L9.3 14.7L0 12L9.3 9.3L12 0Z" fill="url(#geminiGradient)"></path>
              <defs>
                <linearGradient id="geminiGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#4285F4"></stop>
                  <stop offset="30%" stopColor="#9B72CB"></stop>
                  <stop offset="70%" stopColor="#D96570"></stop>
                  <stop offset="100%" stopColor="#F3AF3D"></stop>
                </linearGradient>
              </defs>
            </svg>
            <span className="logo-text">Gemini</span>
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
        <div className="sidebar-nav">
          <button className="nav-item" title="Imágenes">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
              <circle cx="8.5" cy="8.5" r="1.5"></circle>
              <polyline points="21 15 16 10 5 21"></polyline>
            </svg>
            {!isCollapsed && <span>Imágenes</span>}
          </button>
          <button className="nav-item" title="Videos">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="23 7 16 12 23 17 23 7"></polygon>
              <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
            </svg>
            {!isCollapsed && <span>Videos</span>}
          </button>
          <button className="nav-item" title="Biblioteca">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
            </svg>
            {!isCollapsed && <span>Biblioteca</span>}
          </button>
        </div>

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

        {/* Notebooks Section */}
        <div className="sidebar-section">
          {!isCollapsed && <h3 className="sidebar-section-title">Cuadernos</h3>}
          <button className="nav-item notebook-btn" title="Nuevo cuaderno">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            {!isCollapsed && <span>Nuevo cuaderno</span>}
          </button>
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
        <div className="profile-container" title="Dario Vergara (Pro)">
          <div className="profile-avatar">D</div>
          {!isCollapsed && (
            <div className="profile-info">
              <span className="profile-name">Dario Vergara</span>
              <span className="profile-badge">Pro</span>
            </div>
          )}
        </div>
        {!isCollapsed && (
          <button className="btn-icon btn-settings" title="Configuración">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="3"></circle>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
            </svg>
          </button>
        )}
      </div>
    </aside>
  );
};
