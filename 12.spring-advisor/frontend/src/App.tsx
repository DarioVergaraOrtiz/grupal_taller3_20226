import React, { useState, useEffect, useMemo } from 'react';
import { Sidebar } from './components/Sidebar';
import { ChatArea } from './components/ChatArea';
import { ChatInput } from './components/ChatInput';
import { Session, Message } from './types/chat';
import sellosUce from './assets/sellos-uce.png';
import logoComputacion from './assets/logoComputacion.png';
import { InteractiveParticles } from './components/InteractiveParticles';

const API_BASE_URL = 'http://localhost:8091';

export const App: React.FC = () => {
  const [sessions, setSessions] = useState<Session[]>(() => {
    const saved = localStorage.getItem('sessions');
    return saved ? JSON.parse(saved) : [];
  });
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState<boolean>(() => {
    return localStorage.getItem('sidebarCollapsed') === 'true';
  });
  const [selectedModel, setSelectedModel] = useState<'pro' | 'flash'>('pro');
  const [searchQuery, setSearchQuery] = useState('');
  const [isOffline, setIsOffline] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [sessionToDelete, setSessionToDelete] = useState<string | null>(null);
  const [isPreloading, setIsPreloading] = useState(true);
  const [isFadingOut, setIsFadingOut] = useState(false);

  useEffect(() => {
    const fadeTimer = setTimeout(() => {
      setIsFadingOut(true);
    }, 3800);

    const unmountTimer = setTimeout(() => {
      setIsPreloading(false);
    }, 4300);

    return () => {
      clearTimeout(fadeTimer);
      clearTimeout(unmountTimer);
    };
  }, []);

  // Save sessions to LocalStorage on change
  useEffect(() => {
    localStorage.setItem('sessions', JSON.stringify(sessions));
  }, [sessions]);

  // Save sidebar state to LocalStorage
  useEffect(() => {
    localStorage.setItem('sidebarCollapsed', String(isSidebarCollapsed));
  }, [isSidebarCollapsed]);

  // Check connection to backend on mount
  useEffect(() => {
    checkBackendConnection();
  }, []);

  // Initialize session if empty
  useEffect(() => {
    if (sessions.length > 0 && !currentSessionId) {
      setCurrentSessionId(sessions[0].id);
    } else if (sessions.length === 0 && !currentSessionId) {
      startNewSession();
    }
  }, [sessions, currentSessionId]);

  const checkBackendConnection = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/session/new`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });
      if (response.ok) {
        setIsOffline(false);
        // Delete the dummy session created just for testing connection
        const data = await response.json();
        if (data.sessionId) {
          fetch(`${API_BASE_URL}/api/session/${data.sessionId}`, { method: 'DELETE' }).catch(() => {});
        }
      } else {
        setIsOffline(true);
      }
    } catch (e) {
      setIsOffline(true);
    }
  };

  const startNewSession = async () => {
    const generateUUID = () => {
      if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return crypto.randomUUID();
      }
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    };

    try {
      const response = await fetch(`${API_BASE_URL}/api/session/new`, {
        method: 'POST'
      });
      if (!response.ok) throw new Error('Error backend');
      const data = await response.json();
      const newSession: Session = {
        id: data.sessionId,
        name: `Conversación ${sessions.length + 1}`,
        timestamp: new Date().toISOString(),
        messages: []
      };
      setSessions((prev) => [newSession, ...prev]);
      setCurrentSessionId(newSession.id);
      setIsOffline(false);
    } catch (error) {
      console.warn('Backend offline, generando sesión local:', error);
      setIsOffline(true);
      const localId = generateUUID();
      const newSession: Session = {
        id: localId,
        name: `Conversación Local ${sessions.length + 1}`,
        timestamp: new Date().toISOString(),
        messages: []
      };
      setSessions((prev) => [newSession, ...prev]);
      setCurrentSessionId(newSession.id);
    }
  };

  const deleteSession = (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setSessionToDelete(sessionId);
  };

  const confirmDeleteSession = async () => {
    if (!sessionToDelete) return;
    const sessionId = sessionToDelete;
    setSessionToDelete(null);

    try {
      await fetch(`${API_BASE_URL}/api/session/${sessionId}`, {
        method: 'DELETE'
      });
    } catch (e) {
      console.warn('No se pudo borrar del backend:', e);
    }

    const updatedSessions = sessions.filter((s) => s.id !== sessionId);
    setSessions(updatedSessions);

    if (currentSessionId === sessionId) {
      if (updatedSessions.length > 0) {
        setCurrentSessionId(updatedSessions[0].id);
      } else {
        setCurrentSessionId(null); // Will trigger automatic creation in useEffect
      }
    }
  };

  const handleSendMessage = async (text: string) => {
    if (!currentSessionId || isGenerating) return;

    const userMessage: Message = {
      role: 'user',
      content: text,
      timestamp: new Date().toISOString()
    };

    // Update active session locally by adding the user's message
    setSessions((prev) =>
      prev.map((s) => {
        if (s.id === currentSessionId) {
          const updatedMessages = [...s.messages, userMessage];
          // If it was the first message, rename conversation to a snippet of the message
          const newName = s.messages.length === 0
            ? (text.length > 25 ? text.substring(0, 22) + '...' : text)
            : s.name;
          return { ...s, name: newName, messages: updatedMessages };
        }
        return s;
      })
    );

    // Prepare assistant slot
    const assistantPlaceholder: Message = {
      role: 'assistant',
      content: '',
      timestamp: new Date().toISOString()
    };

    setSessions((prev) =>
      prev.map((s) => {
        if (s.id === currentSessionId) {
          return { ...s, messages: [...s.messages, assistantPlaceholder] };
        }
        return s;
      })
    );

    setIsGenerating(true);
    let fullReply = '';
    let inactivityTimeoutId: any;

    try {
      // If offline, simulate local response to keep the educational app functional!
      if (isOffline) {
        await new Promise((resolve) => setTimeout(resolve, 800));
        const localAnswers = [
          "¡Hola! Actualmente el servidor RAG está desconectado. Como asistente local educativo UCE, puedo decirte que para la titulación en la Universidad Central del Ecuador debes cumplir con la suficiencia de idioma extranjero, vinculación con la sociedad, y aprobar tus asignaturas.",
          "El formato del proyecto integrador consta de: Portada, Resumen, Introducción, Metodología, Resultados, Conclusiones y Referencias en formato APA 7ma Edición.",
          "Las fechas de entrega del proyecto integrador suelen publicarse en el cronograma académico oficial de tu facultad. Te sugiero reconectar el servidor para consultar plazos específicos en la base de datos."
        ];
        // Select answer based on prompt keywords
        let replyText = localAnswers[0];
        if (text.toLowerCase().includes('proyecto') || text.toLowerCase().includes('integrador')) {
          replyText = localAnswers[1];
        } else if (text.toLowerCase().includes('plazo') || text.toLowerCase().includes('fecha') || text.toLowerCase().includes('cronograma')) {
          replyText = localAnswers[2];
        }

        // Stream the response locally for fidelity
        const words = replyText.split(' ');
        for (let i = 0; i < words.length; i++) {
          await new Promise((resolve) => setTimeout(resolve, 40));
          fullReply += (i === 0 ? '' : ' ') + words[i];
          setSessions((prev) =>
            prev.map((s) => {
              if (s.id === currentSessionId) {
                const msgs = [...s.messages];
                msgs[msgs.length - 1] = {
                  ...msgs[msgs.length - 1],
                  content: fullReply
                };
                return { ...s, messages: msgs };
              }
              return s;
            })
          );
        }
        setIsGenerating(false);
        return;
      }

      const controller = new AbortController();
      inactivityTimeoutId = setTimeout(() => controller.abort("El servidor tardó demasiado en responder."), 180000);

      const resetTimeout = () => {
        clearTimeout(inactivityTimeoutId);
        inactivityTimeoutId = setTimeout(() => controller.abort("El servidor tardó demasiado en responder."), 180000);
      };

      const response = await fetch(`${API_BASE_URL}/api/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: text,
          sessionId: currentSessionId,
          model: selectedModel
        }),
        signal: controller.signal
      });

      if (!response.ok) throw new Error(`Servidor respondió con código ${response.status}`);

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      if (reader) {
        while (true) {
          const { value, done } = await reader.read();
          if (done) break;

          resetTimeout();
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            const trimmed = line.trim();
            if (!trimmed) continue;

            if (trimmed.startsWith('data:')) {
              const base64Data = trimmed.substring(5).trim();
              if (base64Data === '[DONE]') break;

              try {
                // Decode base64 UTF-8 string safely
                const decodedToken = decodeURIComponent(
                  escape(window.atob(base64Data))
                );
                fullReply += decodedToken;

                setSessions((prev) =>
                  prev.map((s) => {
                    if (s.id === currentSessionId) {
                      const msgs = [...s.messages];
                      msgs[msgs.length - 1] = {
                        ...msgs[msgs.length - 1],
                        content: fullReply
                      };
                      return { ...s, messages: msgs };
                    }
                    return s;
                  })
                );
              } catch (e) {
                console.error('Error al decodificar base64:', e);
              }
            }
          }
        }
      }
    } catch (error: any) {
      console.error('Error al transmitir respuesta:', error);
      
      let errorMsg = error.message || error;
      if (error.name === 'AbortError' || error === "El servidor tardó demasiado en responder.") {
        errorMsg = "El servidor tardó demasiado en responder o la conexión se cerró inesperadamente";
      }

      setSessions((prev) =>
        prev.map((s) => {
          if (s.id === currentSessionId) {
            const msgs = [...s.messages];
            msgs[msgs.length - 1] = {
              ...msgs[msgs.length - 1],
              content: `⚠️ Ocurrió un error con el servidor: ${errorMsg}. Por favor, verifica tu conexión o el estado del LLM.`
            };
            return { ...s, messages: msgs };
          }
          return s;
        })
      );
    } finally {
      if (inactivityTimeoutId) clearTimeout(inactivityTimeoutId);
      setIsGenerating(false);
    }
  };

  const handleCardClick = (prompt: string) => {
    handleSendMessage(prompt);
  };

  // Filter sessions in sidebar using searchQuery
  const filteredSessions = useMemo(() => {
    if (!searchQuery.trim()) return sessions;
    const query = searchQuery.toLowerCase();
    return sessions.filter((s) => s.name.toLowerCase().includes(query));
  }, [sessions, searchQuery]);

  // Find active messages
  const activeSession = sessions.find((s) => s.id === currentSessionId);
  const activeMessages = activeSession?.messages || [];

  if (isPreloading) {
    return (
      <div id="preloader-splash" className={isFadingOut ? 'fade-out' : ''}>
        <div className="preloader-logos-container">
          <img className="logo_load_splash logo-sello" src={sellosUce} alt="Sello UCE" />
          <img className="logo_load_splash logo-computacion" src={logoComputacion} alt="Logo Computación" />
        </div>
        <div className="preloader-wrapper">
          <div className="preloader-slash"></div>
          <div className="preloader-sides">
            <div className="preloader-side"></div>
            <div className="preloader-side"></div>
            <div className="preloader-side"></div>
            <div className="preloader-side"></div>
          </div>
          <div className="preloader-text">
            <div className="preloader-text--backing">CARRERA DE COMPUTACIÓN</div>
            <div className="preloader-text--left">
              <div className="preloader-inner">CARRERA DE COMPUTACIÓN</div>
            </div>
            <div className="preloader-text--right">
              <div className="preloader-inner">CARRERA DE COMPUTACIÓN</div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div id="app" className="gemini-app">
      <InteractiveParticles />
      <Sidebar
        sessions={filteredSessions}
        currentSessionId={currentSessionId}
        isCollapsed={isSidebarCollapsed}
        onToggleCollapse={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
        onSelectSession={setCurrentSessionId}
        onNewChat={startNewSession}
        onDeleteSession={deleteSession}
        onSearchChange={setSearchQuery}
        searchQuery={searchQuery}
        isOffline={isOffline}
        onReconnect={checkBackendConnection}
      />

      {/* Backdrop overlay for mobile when sidebar is open */}
      {!isSidebarCollapsed && (
        <div 
          className="sidebar-backdrop" 
          onClick={() => setIsSidebarCollapsed(true)}
          title="Cerrar menú"
        />
      )}

      <main className="main-content-gemini">
        {/* Toggle Sidebar Button for Mobile View */}
        <button 
          className="btn-toggle-sidebar-mobile" 
          onClick={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
          title="Ver menú"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>

        {/* Edit/New chat button on the top right */}
        <div className="top-header-actions">
          <button className="btn-header-new" onClick={startNewSession} title="Nueva conversación">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
          </button>
        </div>

        {/* Central Chat Feed */}
        <ChatArea
          messages={activeMessages}
          isGenerating={isGenerating}
          onCardClick={handleCardClick}
        />

        {/* Input Bar */}
        <ChatInput
          onSendMessage={handleSendMessage}
          isLoading={isGenerating}
          selectedModel={selectedModel}
          onModelChange={setSelectedModel}
        />
      </main>

      {/* Custom Modal for Session Deletion */}
      {sessionToDelete && (
        <div className="modal-overlay-gemini">
          <div className="modal-content-gemini">
            <h3 className="modal-title">¿Eliminar conversación?</h3>
            <p className="modal-description">
              Esta conversación se borrará de forma permanente de tu historial de recientes.
            </p>
            <div className="modal-actions-gemini">
              <button 
                className="modal-btn btn-cancel" 
                onClick={() => setSessionToDelete(null)}
              >
                Cancelar
              </button>
              <button 
                className="modal-btn btn-confirm" 
                onClick={confirmDeleteSession}
              >
                Eliminar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
