// ═══════════════════════════════════════════════════════
//  NexusChat — WebSocket Client
// ═══════════════════════════════════════════════════════

(() => {
    'use strict';

    // ─── State ───
    let ws = null;
    let username = null;
    let currentRoom = null;
    const userColors = {};
    let colorIndex = 0;

    // ─── DOM Elements ───
    const $ = id => document.getElementById(id);

    const loginScreen   = $('login-screen');
    const chatScreen    = $('chat-screen');
    const usernameInput = $('username-input');
    const loginBtn      = $('login-btn');
    const loginError    = $('login-error');

    const userBadge     = $('user-badge');
    const userAvatar    = $('user-avatar');
    const userDisplay   = $('user-display');
    const roomList      = $('room-list');
    const userList      = $('user-list');
    const userCount     = $('user-count');
    const createRoomBtn = $('create-room-btn');
    const logoutBtn     = $('logout-btn');

    const noRoom        = $('no-room');
    const activeChat    = $('active-chat');
    const currentRoomEl = $('current-room-name');
    const messagesEl    = $('messages');
    const messageInput  = $('message-input');
    const sendBtn       = $('send-btn');
    const leaveRoomBtn  = $('leave-room-btn');

    const roomModal     = $('room-modal');
    const roomNameInput = $('room-name-input');
    const modalCancel   = $('modal-cancel');
    const modalJoin     = $('modal-join');

    // ─── WebSocket ───

    function connect() {
        const host = window.location.hostname || 'localhost';
        ws = new WebSocket(`ws://${host}:9091`);

        ws.onopen = () => {
            console.log('WebSocket connected');
        };

        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                handleServerMessage(data);
            } catch (e) {
                console.error('Bad message:', event.data);
            }
        };

        ws.onclose = () => {
            console.log('WebSocket disconnected');
            if (username) {
                toast('Disconnected from server', 'error');
                setTimeout(() => {
                    resetToLogin();
                }, 2000);
            }
        };

        ws.onerror = () => {
            toast('Connection error', 'error');
        };
    }

    function send(obj) {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify(obj));
        }
    }

    // ─── Server Message Router ───

    function handleServerMessage(data) {
        switch (data.type) {
            case 'auth_ok':
                onAuthOk(data);
                break;
            case 'auth_fail':
                onAuthFail(data);
                break;
            case 'joined':
                onJoined(data);
                break;
            case 'left':
                onLeft(data);
                break;
            case 'room_list':
                onRoomList(data);
                break;
            case 'user_list':
                onUserList(data);
                break;
            case 'chat':
                onChat(data);
                break;
            case 'system':
                addSystemMessage(data.content || '');
                break;
            case 'error':
                toast(data.content || 'Server error', 'error');
                break;
        }
    }

    // ─── Auth ───

    function onAuthOk(data) {
        username = data.username;
        loginScreen.classList.remove('active');
        chatScreen.classList.add('active');

        userDisplay.textContent = username;
        const initial = username.charAt(0).toUpperCase();
        userAvatar.textContent = initial;
        userAvatar.className = 'user-avatar avatar-' + (hashCode(username) % 8 + 1);

        loginBtn.disabled = false;
        loginBtn.querySelector('span').textContent = 'Enter Chat';
        toast(`Welcome, ${username}!`, 'success');
    }

    function onAuthFail(data) {
        showLoginError(data.error || 'Authentication failed');
        loginBtn.disabled = false;
        loginBtn.querySelector('span').textContent = 'Enter Chat';
    }

    // ─── Room Events ───

    function onJoined(data) {
        currentRoom = data.room;
        noRoom.classList.add('hidden');
        activeChat.classList.remove('hidden');
        currentRoomEl.textContent = data.room;
        messagesEl.innerHTML = '';
        addSystemMessage(`You joined #${data.room}`);
        messageInput.focus();
        closeModal();
    }

    function onLeft(data) {
        addSystemMessage(`You left #${data.room}`);
        currentRoom = null;
        activeChat.classList.add('hidden');
        noRoom.classList.remove('hidden');
    }

    function onRoomList(data) {
        const rooms = data.rooms || [];
        if (rooms.length === 0) {
            roomList.innerHTML = '<div class="empty-state">No rooms yet. Create one!</div>';
            return;
        }
        roomList.innerHTML = rooms.map(r => {
            const active = r.name === currentRoom ? ' active' : '';
            return `<div class="room-item${active}" data-room="${escapeHtml(r.name)}">
                <span class="room-name">${escapeHtml(r.name)}</span>
                <span class="room-members">${r.members}</span>
            </div>`;
        }).join('');

        // Click handlers
        roomList.querySelectorAll('.room-item').forEach(el => {
            el.addEventListener('click', () => {
                const room = el.dataset.room;
                if (room !== currentRoom) {
                    send({ type: 'join', room });
                }
            });
        });
    }

    function onUserList(data) {
        const users = data.users || [];
        userCount.textContent = users.length;
        userList.innerHTML = users.map(u => {
            const isSelf = u === username ? ' self' : '';
            return `<div class="user-item${isSelf}">
                <span class="status-dot"></span>
                <span>${escapeHtml(u)}${u === username ? ' (you)' : ''}</span>
            </div>`;
        }).join('');
    }

    // ─── Chat Messages ───

    function onChat(data) {
        addChatMessage(data.sender, data.content, data.timestamp);
    }

    function addChatMessage(sender, content, timestamp) {
        const div = document.createElement('div');
        div.className = 'msg';

        const color = getUserColor(sender);
        const time = formatTime(timestamp);

        div.innerHTML = `
            <div class="msg-header">
                <span class="msg-author" style="color: ${color}">${escapeHtml(sender)}</span>
                <span class="msg-time">${time}</span>
            </div>
            <div class="msg-body">${escapeHtml(content)}</div>
        `;
        messagesEl.appendChild(div);
        scrollToBottom();
    }

    function addSystemMessage(text) {
        const div = document.createElement('div');
        div.className = 'msg msg-system';
        div.innerHTML = `<div class="msg-body">${escapeHtml(text)}</div>`;
        messagesEl.appendChild(div);
        scrollToBottom();
    }

    function scrollToBottom() {
        requestAnimationFrame(() => {
            messagesEl.scrollTop = messagesEl.scrollHeight;
        });
    }

    // ─── Event Listeners ───

    // Login
    loginBtn.addEventListener('click', doLogin);
    usernameInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') doLogin();
    });

    function doLogin() {
        const name = usernameInput.value.trim();
        if (!name) {
            showLoginError('Please enter a username');
            return;
        }
        hideLoginError();
        loginBtn.disabled = true;
        loginBtn.querySelector('span').textContent = 'Connecting...';
        connect();

        // Wait for connection then authenticate
        const checkInterval = setInterval(() => {
            if (ws && ws.readyState === WebSocket.OPEN) {
                clearInterval(checkInterval);
                send({ type: 'auth', username: name });
            }
        }, 100);

        // Timeout after 5 seconds
        setTimeout(() => {
            clearInterval(checkInterval);
            if (!username) {
                loginBtn.disabled = false;
                loginBtn.querySelector('span').textContent = 'Enter Chat';
                showLoginError('Could not connect to server');
            }
        }, 5000);
    }

    // Send message
    sendBtn.addEventListener('click', doSend);
    messageInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') doSend();
    });

    function doSend() {
        const content = messageInput.value.trim();
        if (!content || !currentRoom) return;
        send({ type: 'chat', content });
        messageInput.value = '';
        messageInput.focus();
    }

    // Create room modal
    createRoomBtn.addEventListener('click', () => {
        roomModal.classList.remove('hidden');
        roomNameInput.value = '';
        roomNameInput.focus();
    });

    modalCancel.addEventListener('click', closeModal);
    roomModal.querySelector('.modal-backdrop').addEventListener('click', closeModal);
    roomNameInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') doJoinRoom();
        if (e.key === 'Escape') closeModal();
    });
    modalJoin.addEventListener('click', doJoinRoom);

    function doJoinRoom() {
        const room = roomNameInput.value.trim();
        if (!room) return;
        send({ type: 'join', room });
    }

    function closeModal() {
        roomModal.classList.add('hidden');
    }

    // Leave room
    leaveRoomBtn.addEventListener('click', () => {
        send({ type: 'leave' });
    });

    // Logout
    logoutBtn.addEventListener('click', () => {
        if (ws) ws.close();
        resetToLogin();
    });

    // ─── Helpers ───

    function resetToLogin() {
        username = null;
        currentRoom = null;
        chatScreen.classList.remove('active');
        loginScreen.classList.add('active');
        loginBtn.disabled = false;
        loginBtn.querySelector('span').textContent = 'Enter Chat';
        usernameInput.value = '';
        hideLoginError();
    }

    function showLoginError(msg) {
        loginError.textContent = msg;
        loginError.classList.remove('hidden');
    }

    function hideLoginError() {
        loginError.classList.add('hidden');
    }

    function toast(msg, type = 'info') {
        const container = $('toast-container');
        const el = document.createElement('div');
        el.className = `toast ${type}`;
        el.textContent = msg;
        container.appendChild(el);
        setTimeout(() => {
            el.style.animation = 'toast-out 0.3s ease forwards';
            setTimeout(() => el.remove(), 300);
        }, 3000);
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function formatTime(ts) {
        if (!ts) return new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const d = new Date(ts);
        return isNaN(d) ? '' : d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    const COLORS = [
        '#6c63ff', '#00d4aa', '#ff6b9d', '#4da6ff',
        '#ff9f43', '#a29bfe', '#55efc4', '#fd79a8',
        '#74b9ff', '#ffeaa7', '#dfe6e9', '#e17055'
    ];

    function getUserColor(name) {
        if (!userColors[name]) {
            userColors[name] = COLORS[colorIndex % COLORS.length];
            colorIndex++;
        }
        return userColors[name];
    }

    function hashCode(str) {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            hash = ((hash << 5) - hash) + str.charCodeAt(i);
            hash |= 0;
        }
        return Math.abs(hash);
    }

})();
