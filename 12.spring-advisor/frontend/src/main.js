import './style.css';

// Backend API base URL
const API_BASE_URL = 'http://localhost:8091';

// State Management
let currentSessionId = null;
let sessions = JSON.parse(localStorage.getItem('sessions') || '[]');

// DOM Elements
const sidebar = document.getElementById('sidebar');
const btnToggleSidebar = document.getElementById('btn-toggle-sidebar');
const btnNewChat = document.getElementById('btn-new-chat');
const sessionsList = document.getElementById('sessions-list');
const chatArea = document.getElementById('chat-area');
const welcomeScreen = document.getElementById('welcome-screen');
const messagesContainer = document.getElementById('messages-container');
const messageInput = document.getElementById('message-input');
const btnSend = document.getElementById('btn-send');
const sessionIndicator = document.getElementById('session-indicator');
const charCount = document.getElementById('char-count');

// Initialize application
document.addEventListener('DOMContentLoaded', () => {
    setupEventListeners();
    renderSessions();
    
    if (sessions.length > 0) {
        // Load the most recent session
        switchSession(sessions[0].id);
    } else {
        startNewSession();
    }
});

// Configure Event Listeners
function setupEventListeners() {
    // Mobile sidebar toggle
    btnToggleSidebar?.addEventListener('click', () => {
        sidebar.classList.toggle('open');
    });

    // New Chat Button
    btnNewChat?.addEventListener('click', () => {
        startNewSession();
        if (window.innerWidth <= 768) {
            sidebar.classList.remove('open');
        }
    });

    // Send Button
    btnSend?.addEventListener('click', handleSendMessage);

    // Textarea input listeners
    messageInput?.addEventListener('input', () => {
        // Auto-resize textarea
        messageInput.style.height = 'auto';
        messageInput.style.height = `${Math.min(messageInput.scrollHeight - 12, 150)}px`;
        
        // Character count
        const length = messageInput.value.length;
        charCount.textContent = `${length} / 2000`;
        
        // Disable/enable send button
        btnSend.disabled = length === 0;
    });

    // Handle Enter to send, Shift+Enter for newline
    messageInput?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (!btnSend.disabled) {
                handleSendMessage();
            }
        }
    });

    // Welcome Screen suggested cards
    document.querySelectorAll('.welcome-card').forEach(card => {
        card.addEventListener('click', () => {
            const prompt = card.getAttribute('data-prompt');
            if (prompt) {
                messageInput.value = prompt;
                messageInput.dispatchEvent(new Event('input'));
                handleSendMessage();
            }
        });
    });
}

// Generate or fetch new session
async function startNewSession() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/session/new`, {
            method: 'POST'
        });
        
        if (!response.ok) throw new Error('Error al crear sesión en backend');
        
        const data = await response.json();
        const sessionId = data.sessionId;
        
        const newSession = {
            id: sessionId,
            name: `Conversación ${sessions.length + 1}`,
            timestamp: new Date().toISOString(),
            messages: []
        };
        
        sessions.unshift(newSession);
        saveSessionsToStorage();
        renderSessions();
        switchSession(sessionId);
    } catch (error) {
        console.error('Error al iniciar sesión:', error);
        // Fallback locally generated UUID if backend is offline
        const localSessionId = crypto.randomUUID();
        const newSession = {
            id: localSessionId,
            name: `Conversación Local ${sessions.length + 1}`,
            timestamp: new Date().toISOString(),
            messages: []
        };
        sessions.unshift(newSession);
        saveSessionsToStorage();
        renderSessions();
        switchSession(localSessionId);
    }
}

// Switch active session
function switchSession(sessionId) {
    currentSessionId = sessionId;
    sessionIndicator.textContent = `Sesión: ${sessionId.substring(0, 8)}...`;
    
    // Update active UI class in list
    document.querySelectorAll('.session-item').forEach(item => {
        if (item.getAttribute('data-id') === sessionId) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });
    
    // Load and render messages for this session
    const session = sessions.find(s => s.id === sessionId);
    messagesContainer.innerHTML = '';
    
    if (session && session.messages && session.messages.length > 0) {
        welcomeScreen.classList.add('hidden');
        session.messages.forEach(msg => {
            addMessageToDOM(msg.role, msg.content);
        });
    } else {
        welcomeScreen.classList.remove('hidden');
    }
    
    scrollToBottom();
}

// Delete session
async function deleteSession(sessionId, event) {
    event.stopPropagation(); // Avoid triggering switchSession
    
    if (!confirm('¿Estás seguro de que deseas eliminar esta conversación?')) return;
    
    try {
        // Notify backend to clear memory
        await fetch(`${API_BASE_URL}/api/session/${sessionId}`, {
            method: 'DELETE'
        });
    } catch (e) {
        console.warn('No se pudo borrar del backend, eliminando localmente:', e);
    }
    
    sessions = sessions.filter(s => s.id !== sessionId);
    saveSessionsToStorage();
    renderSessions();
    
    if (currentSessionId === sessionId) {
        if (sessions.length > 0) {
            switchSession(sessions[0].id);
        } else {
            startNewSession();
        }
    }
}

// Render sessions in sidebar
function renderSessions() {
    sessionsList.innerHTML = '';
    
    sessions.forEach(session => {
        const item = document.createElement('div');
        item.className = `session-item ${session.id === currentSessionId ? 'active' : ''}`;
        item.setAttribute('data-id', session.id);
        item.addEventListener('click', () => switchSession(session.id));
        
        const label = document.createElement('span');
        label.className = 'session-label';
        label.textContent = session.name;
        
        const btnDelete = document.createElement('button');
        btnDelete.className = 'session-delete';
        btnDelete.innerHTML = `
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>
            </svg>
        `;
        btnDelete.addEventListener('click', (e) => deleteSession(session.id, e));
        
        item.appendChild(label);
        item.appendChild(btnDelete);
        sessionsList.appendChild(item);
    });
}

// Save sessions to localStorage
function saveSessionsToStorage() {
    localStorage.setItem('sessions', JSON.stringify(sessions));
}

// Add message to HTML DOM
function addMessageToDOM(role, content) {
    welcomeScreen.classList.add('hidden');
    
    const messageEl = document.createElement('div');
    messageEl.className = `message ${role}`;
    
    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.textContent = role === 'user' ? 'U' : 'AI';
    
    const contentEl = document.createElement('div');
    contentEl.className = 'message-content';
    
    if (role === 'assistant') {
        // Safe Markdown rendering using marked
        contentEl.innerHTML = window.marked ? window.marked.parse(content) : content;
    } else {
        contentEl.textContent = content;
    }
    
    messageEl.appendChild(avatar);
    messageEl.appendChild(contentEl);
    messagesContainer.appendChild(messageEl);
    scrollToBottom();
    
    return contentEl;
}

// Handle sending message & stream SSE response
async function handleSendMessage() {
    const text = messageInput.value.trim();
    if (!text) return;
    
    // Disable inputs
    messageInput.value = '';
    messageInput.style.height = 'auto';
    messageInput.disabled = true;
    btnSend.disabled = true;
    charCount.textContent = '0 / 2000';
    
    // Add user message
    addMessageToDOM('user', text);
    saveMessageToSession('user', text);
    
    // Update session label to reflect first message if generic
    const currentSession = sessions.find(s => s.id === currentSessionId);
    if (currentSession && currentSession.messages.length === 1) {
        currentSession.name = text.length > 25 ? text.substring(0, 22) + '...' : text;
        saveSessionsToStorage();
        renderSessions();
    }
    
    // Create assistant typing message bubble
    const assistantContentEl = addMessageToDOM('assistant', '');
    const typingIndicator = document.createElement('div');
    typingIndicator.className = 'typing-indicator';
    typingIndicator.innerHTML = '<span></span><span></span><span></span>';
    assistantContentEl.appendChild(typingIndicator);
    
    let fullReply = '';
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: text,
                sessionId: currentSessionId
            })
        });
        
        if (!response.ok) {
            throw new Error(`Error en servidor: ${response.statusText}`);
        }
        
        // Remove typing indicator when response starts streaming
        assistantContentEl.innerHTML = '';
        
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        
        while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            
            buffer += decoder.decode(value, { stream: true });
            
            // Process lines in buffer
            const lines = buffer.split('\n');
            buffer = lines.pop(); // Keep last incomplete line in buffer
            
            for (const line of lines) {
                const trimmed = line.trim();
                if (!trimmed) continue;
                
                if (trimmed.startsWith('event:')) {
                    // Extract event type if needed
                    continue;
                }
                
                if (trimmed.startsWith('data:')) {
                    const base64Data = trimmed.substring(5).trim();
                    
                    if (base64Data === '[DONE]') {
                        break;
                    }
                    
                    try {
                        // Decode base64 UTF-8 string safely
                        const decodedToken = decodeURIComponent(
                            escape(window.atob(base64Data))
                        );
                        
                        fullReply += decodedToken;
                        
                        // Dynamically update content with markdown parsing
                        if (window.marked) {
                            assistantContentEl.innerHTML = window.marked.parse(fullReply);
                        } else {
                            assistantContentEl.textContent = fullReply;
                        }
                        scrollToBottom();
                    } catch (e) {
                        console.error('Error decoding token base64:', e);
                    }
                }
            }
        }
        
        // Complete reading, save to storage
        saveMessageToSession('assistant', fullReply);
        
    } catch (error) {
        console.error('Error streaming chat:', error);
        assistantContentEl.innerHTML = `<span style="color: #ef4444;">Error de comunicación con el backend: ${error.message}</span>`;
    } finally {
        // Re-enable inputs
        messageInput.disabled = false;
        messageInput.focus();
    }
}

// Save message to current session state
function saveMessageToSession(role, content) {
    const session = sessions.find(s => s.id === currentSessionId);
    if (session) {
        if (!session.messages) session.messages = [];
        session.messages.push({ role, content, timestamp: new Date().toISOString() });
        saveSessionsToStorage();
    }
}

// Utility: Auto-scroll to bottom of chat
function scrollToBottom() {
    chatArea.scrollTop = chatArea.scrollHeight;
}
