const chatSessionIdPattern = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$/;

function createChatSessionId() {
  if (crypto.randomUUID) {
    return crypto.randomUUID();
  }

  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;

  const hex = [...bytes].map((byte) => byte.toString(16).padStart(2, "0"));
  return `${hex.slice(0, 4).join("")}-${hex.slice(4, 6).join("")}-${hex.slice(6, 8).join("")}-${hex.slice(8, 10).join("")}-${hex.slice(10).join("")}`;
}

function getChatSessionId() {
  const savedSessionId = localStorage.getItem("chatSessionId");
  return chatSessionIdPattern.test(savedSessionId) ? savedSessionId : createChatSessionId();
}

const state = {
  token: localStorage.getItem("accessToken"),
  products: [],
  searchProducts: [],
  bestProducts: [],
  bestExpanded: false,
  currentProduct: null,
  productKeyword: "",
  productCategoryId: "",
  productSort: "LATEST",
  productPage: 0,
  productTotalPages: 0,
  productTotalElements: 0,
  orderPage: 0,
  orderTotalPages: 0,
  orderTotalElements: 0,
  searchKeyword: "",
  searchCategoryId: "",
  searchLabel: "",
  searchSort: "LATEST",
  searchPage: 0,
  searchTotalPages: 0,
  searchTotalElements: 0,
  chatSessionId: getChatSessionId(),
  chatMode: "ai",
  chatMessages: [],
  chatLoading: false,
  chatTopic: "PRODUCT_RECOMMENDATION",
  currentUser: null,
  liveChatRoom: null,
  liveChatMessages: [],
  liveChatRooms: [],
  liveChatListMode: false,
  liveChatExited: false,
  exitedLiveChatRoomKeys: new Set(JSON.parse(localStorage.getItem("exitedLiveChatRoomKeys") || "[]")),
  liveChatCreating: false,
  liveChatConnecting: false,
  liveChatConnected: false,
  liveChatSubscription: null,
  stompClient: null,
  lastLiveMessageId: 0,
  quantity: 1,
  checkoutItems: [],
  checkoutOrder: null,
  checkoutSource: null,
  checkoutCartItemIds: [],
  selectedUserCouponId: null,
  checkoutAmount: 0
};
localStorage.setItem("chatSessionId", state.chatSessionId);

const chatTopicLabels = {
  PRODUCT_RECOMMENDATION: "상품 추천",
  PRODUCT_SUMMARY: "상품 설명 요약",
  PRE_CART_QUESTION: "장바구니 담기 전 질문",
  PRODUCT_COMPARISON: "특정 상품 비교"
};

const chatTopicPlaceholders = {
  PRODUCT_RECOMMENDATION: "예: 업무용 노트북 추천해줘",
  PRODUCT_SUMMARY: "예: MacBook Air 13 M4 특징을 요약해줘",
  PRE_CART_QUESTION: "예: LG Gram 16 2026 사용 전에 뭘 확인해야 해?",
  PRODUCT_COMPARISON: "예: Logitech MX Keys S랑 Logitech MX Master 3S 차이가 뭐야?"
};

const samples = [
  { id: 1, name: "Galaxy S25 256GB", price: 1290000, stock: 35, status: "ON_SALE", categoryId: 1, categoryName: "스마트폰", description: "6.7인치 AMOLED 디스플레이와 고성능 카메라를 갖춘 스마트폰입니다." },
  { id: 8, name: "MacBook Air 13 M4", price: 1590000, stock: 16, status: "ON_SALE", categoryId: 3, categoryName: "노트북", description: "휴대성과 성능을 모두 갖춘 13형 노트북입니다." },
  { id: 19, name: "AirPods Pro 3", price: 359000, stock: 24, status: "ON_SALE", categoryId: 7, categoryName: "이어폰/헤드폰", description: "노이즈 캔슬링과 공간 음향을 지원하는 무선 이어폰입니다." },
  { id: 24, name: "Apple Watch Series 11", price: 599000, stock: 19, status: "ON_SALE", categoryId: 9, categoryName: "스마트워치", description: "운동 기록과 건강 관리를 지원하는 스마트워치입니다." }
];

const categories = [
  { id: "", name: "전체", description: "모든 상품" },
  { id: "1", name: "스마트폰", description: "자급제 / 플래그십" },
  { id: "2", name: "태블릿", description: "학습 / 필기" },
  { id: "3", name: "노트북", description: "업무 / 게이밍" },
  { id: "4", name: "데스크탑", description: "사무 / 게이밍" },
  { id: "5", name: "모니터", description: "업무 / 게이밍" },
  { id: "6", name: "키보드/마우스", description: "입력장치" },
  { id: "7", name: "이어폰/헤드폰", description: "음향기기" },
  { id: "8", name: "스피커", description: "블루투스 / 홈" },
  { id: "9", name: "스마트워치", description: "운동 / 건강" },
  { id: "10", name: "카메라", description: "촬영 장비" },
  { id: "11", name: "게임기/콘솔", description: "콘솔 게임" },
  { id: "12", name: "저장장치", description: "SSD / 외장하드" },
  { id: "13", name: "네트워크 장비", description: "공유기 / 메시" },
  { id: "14", name: "충전기/케이블", description: "전원 액세서리" },
  { id: "15", name: "PC 부품", description: "CPU / GPU" },
  { id: "16", name: "생활가전", description: "청소 / 관리" },
  { id: "17", name: "주방가전", description: "조리 / 주방" },
  { id: "18", name: "계절가전", description: "제습 / 순환" },
  { id: "19", name: "액세서리", description: "보호 / 케이스" },
  { id: "20", name: "기타 전자제품", description: "테스트 / 기타" }
];

const $ = (selector) => document.querySelector(selector);
const money = (value) => `${Number(value || 0).toLocaleString("ko-KR")}원`;
const NO_COUPON_MESSAGE = "보유한 쿠폰이 없습니다.";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatDateTime(value) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}

function formatDate(value) {
  if (!value) return "-";
  return value.slice(0, 10);
}

function formatDiscount(event) {
  if (!event) return "할인 정보 없음";
  if (event.discountType === "PERCENT") {
    return `${event.discountAmount}% 할인`;
  }
  return `${money(event.discountAmount)} 할인`;
}

function formatOrderStatus(status) {
  const labels = {
    READY: "주문 준비중",
    PAYMENT_PENDING: "결제 대기",
    COMPLETED: "결제 완료",
    CANCELED: "주문 취소"
  };
  return labels[status] || status;
}

function getOrderCancelMessage(status) {
  const messages = {
    PAYMENT_PENDING: "결제 대기 상태일 때는 주문을 취소할 수 없습니다. 상담사에게 문의 주세요.",
    COMPLETED: "결제가 완료되었다면 주문을 취소할 수 없습니다. 상담사에게 문의 주세요."
  };
  return messages[status] || "";
}

function authHeaders() {
  return state.token ? { Authorization: `Bearer ${state.token}` } : {};
}

function clearOrderDetailPanel() {
  $("#orderDetailPanel")?.remove();
}

function expireLogin(message = "로그인이 만료되었습니다. 다시 로그인해주세요.") {
  state.token = null;
  state.currentUser = null;
  disconnectLiveChat();
  state.liveChatRoom = null;
  state.liveChatMessages = [];
  state.liveChatRooms = [];
  state.liveChatListMode = false;
  state.liveChatExited = false;
  localStorage.removeItem("accessToken");
  state.orderPage = 0;
  state.orderTotalPages = 0;
  state.orderTotalElements = 0;
  updateAuthButton();
  $("#orderList").innerHTML = "";
  $("#orderPagination").innerHTML = "";
  $("#cartList").innerHTML = "";
  clearOrderDetailPanel();
  showToast(message);
  showView("auth");
}

async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
      ...(options.headers || {})
    }
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = body.message || "요청을 처리하지 못했습니다.";
    if (response.status === 401 && state.token) {
      expireLogin(message);
    }
    throw new Error(message);
  }
  return body.data ?? body;
}

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2600);
}

function getCurrentUserId() {
  if (state.currentUser?.userId) return Number(state.currentUser.userId);
  if (!state.token) return null;
  try {
    const base64 = state.token.split(".")[1].replaceAll("-", "+").replaceAll("_", "/");
    const paddedBase64 = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    const payload = JSON.parse(atob(paddedBase64));
    return Number(payload.sub);
  } catch (error) {
    return null;
  }
}

function isAdminUser() {
  return state.currentUser?.role === "ADMIN";
}

async function ensureCurrentUser() {
  if (!state.token) return null;
  if (state.currentUser) return state.currentUser;
  state.currentUser = await request("/api/users/me");
  return state.currentUser;
}

function oneMonthAgo() {
  const date = new Date();
  date.setMonth(date.getMonth() - 1);
  return date;
}

function getLiveChatRoomKey(room) {
  if (!room) return "";
  return `${room.roomId}:${room.createdAt || ""}`;
}

function rememberExitedLiveChatRoom(room) {
  const roomKey = getLiveChatRoomKey(room);
  if (!roomKey) return;
  state.exitedLiveChatRoomKeys.add(roomKey);
  localStorage.setItem("exitedLiveChatRoomKeys", JSON.stringify([...state.exitedLiveChatRoomKeys]));
}

function renderChatMessages() {
  const messages = $("#chatMessages");
  if (state.chatMode === "live" && state.liveChatListMode) {
    renderLiveChatRoomList();
    return;
  }

  const currentUserId = getCurrentUserId();
  const items = state.chatMode === "ai"
    ? state.chatMessages.map((message) => ({
        sender: message.sender,
        text: message.text
      }))
    : state.liveChatMessages.map((message) => ({
        sender: getLiveMessageSender(message, currentUserId),
        name: message.senderName,
        text: normalizeLiveMessageText(message)
      }));

  const notice = state.chatMode === "live" && !isAdminUser()
    ? `<div class="chat-history-note">1:1 채팅은 1개월 간의 기록만 보여집니다.</div>`
    : "";
  const empty = state.chatMode === "live" && !items.length
    ? `<div class="chat-empty">채팅 기록이 없습니다.</div>`
    : "";

  messages.innerHTML = notice + empty + items.map((message) => `
    <div class="chat-message ${message.sender}">
      ${message.name ? `<span>${escapeHtml(message.name)}</span>` : ""}
      <p>${escapeHtml(message.text)}</p>
    </div>
  `).join("");
  messages.scrollTop = messages.scrollHeight;
}

function renderLiveChatRoomList() {
  const messages = $("#chatMessages");
  if (!state.liveChatRooms.length) {
    messages.innerHTML = `<div class="chat-empty">${isAdminUser() ? "담당 가능한 채팅이 없습니다." : "채팅 기록이 없습니다."}</div>`;
    return;
  }

  messages.innerHTML = state.liveChatRooms.map((room) => {
    const exited = isExitedLiveChatRoom(room);
    return `
    <button class="chat-room-item ${getChatRoomItemClass(room)}" type="button" data-chat-room-id="${room.roomId}">
      <span>
        <strong>${escapeHtml(room.name)}</strong>
        <small>${escapeHtml(formatDateTime(room.createdAt))} · ${escapeHtml(formatChatStatus(exited ? "COMPLETED" : room.status))}</small>
      </span>
      <b>${escapeHtml(getChatRoomBadge(room))}</b>
    </button>
  `;
  }).join("");
}

function isExitedLiveChatRoom(room) {
  return state.exitedLiveChatRoomKeys.has(getLiveChatRoomKey(room));
}

function getChatRoomItemClass(room) {
  if (isExitedLiveChatRoom(room) || room.status === "COMPLETED") {
    return "ended";
  }
  return room.adminId ? "" : "waiting";
}

function getChatRoomBadge(room) {
  if (isExitedLiveChatRoom(room) || room.status === "COMPLETED") {
    return "상담 종료";
  }
  if (isAdminUser()) {
    return room.adminId ? room.adminName || "담당자 배정" : "미배정";
  }
  return room.adminName || "상담 대기";
}

function formatChatStatus(status) {
  const labels = {
    WAITING: "대기중",
    IN_PROGRESS: "상담중",
    COMPLETED: "종료"
  };
  return labels[status] || status;
}

function addChatMessage(sender, text) {
  state.chatMessages.push({ sender, text });
  renderChatMessages();
}

function getLiveMessageSender(message, currentUserId) {
  if (message.messageType === "SYSTEM") return "system";
  return currentUserId && Number(message.senderId) === currentUserId ? "user" : "bot";
}

function addLiveMessage(message) {
  if (state.chatMode === "live" && !isAdminUser() && !isVisibleCustomerLiveMessage(message)) {
    return;
  }
  if (message.messageId && state.liveChatMessages.some((item) => item.messageId === message.messageId)) {
    return;
  }
  state.liveChatMessages.push(message);
  state.liveChatMessages.sort((a, b) => {
    if (a.messageId && b.messageId) return a.messageId - b.messageId;
    return String(a.createdAt || "").localeCompare(String(b.createdAt || ""));
  });
  state.lastLiveMessageId = Math.max(state.lastLiveMessageId, Number(message.messageId || 0));
  renderChatMessages();
}

function isVisibleCustomerLiveMessage(message) {
  if (message.messageType !== "SYSTEM") {
    return true;
  }
  if (message.local) {
    return true;
  }

  const currentUserId = getCurrentUserId();
  return currentUserId
    && Number(message.senderId) === currentUserId
    && String(message.content || "").includes("퇴장");
}

function normalizeLiveMessageText(message) {
  if (message.messageType === "SYSTEM" && String(message.content || "").includes("퇴장했습니다")) {
    return String(message.content).replace("퇴장했습니다", "퇴장하셨습니다.");
  }

  return message.content;
}

function setChatOpen(isOpen) {
  $("#chatPanel").classList.toggle("hidden", !isOpen);
  $("#chatToggleButton").classList.toggle("hidden", isOpen);
  $("#chatToggleButton").setAttribute("aria-expanded", String(isOpen));
  if (isOpen && state.chatMode === "ai" && !state.chatMessages.length) {
    addChatMessage("bot", "상담 유형을 선택한 뒤 질문해주세요.\n상품 추천, 상품 설명 요약, 장바구니 담기 전 질문, 특정 상품 비교만 도와드릴 수 있습니다.");
  }
  if (isOpen && state.chatMode === "live") {
    startLiveChat();
  }
  if (isOpen) {
    $("#chatInput").focus();
  }
}

async function openLiveChatByRole() {
  try {
    await ensureCurrentUser();
    disconnectLiveChat();
    state.liveChatRoom = null;
    state.liveChatListMode = false;
    state.liveChatExited = false;
    setChatMode("live");
  } catch (error) {
    showToast(error.message);
    setChatMode("ai");
  } finally {
    $("#chatInput").focus();
  }
}

async function sendChatMessage(message) {
  if (state.chatLoading) return;
  state.chatLoading = true;
  $("#chatSendButton").disabled = true;
  addChatMessage("user", message);
  $("#chatInput").value = "";

  try {
    const response = await request("/api/chatbot", {
      method: "POST",
      body: JSON.stringify({
        sessionId: state.chatSessionId,
        topic: state.chatTopic,
        message
      })
    });
    addChatMessage("bot", response.message || "답변을 받지 못했습니다.");
  } catch (error) {
    addChatMessage("bot", error.message);
  } finally {
    state.chatLoading = false;
    $("#chatSendButton").disabled = false;
    $("#chatInput").focus();
  }
}

function setChatTopic(topic, options = {}) {
  state.chatTopic = topic;
  document.querySelectorAll("[data-chat-topic]").forEach((button) => {
    button.classList.toggle("active", button.dataset.chatTopic === topic);
  });
  $("#chatInput").placeholder = chatTopicPlaceholders[topic] || "궁금한 내용을 입력해주세요";
  if (options.notify !== false) {
    showToast(`${chatTopicLabels[topic]} 상담으로 변경했습니다.`);
  }
}

function setChatMode(mode) {
  state.chatMode = mode;
  document.querySelectorAll("[data-chat-mode]").forEach((button) => {
    const isActive = button.dataset.chatMode === mode;
    button.classList.toggle("active", isActive);
    button.setAttribute("aria-selected", String(isActive));
  });
  $("#chatTopics").classList.toggle("hidden", mode !== "ai");
  $("#chatStatusText").textContent = mode === "ai"
    ? "상품 선택을 도와드릴게요."
    : getLiveChatStatusText();
  $("#chatInput").placeholder = mode === "ai"
    ? chatTopicPlaceholders[state.chatTopic]
    : "상담사에게 문의할 내용을 입력해주세요";
  $("#chatSendButton").disabled = mode === "live" && state.liveChatConnecting;
  updateLiveChatControls();
  renderChatMessages();

  if (mode === "live") {
    startLiveChat();
  }
}

function getLiveChatStatusText() {
  if (!state.token) return "로그인한 회원만 1:1 문의를 이용할 수 있습니다.";
  if (isEmptyCustomerLiveChatList()) return "새 문의를 시작할 수 있습니다.";
  if (state.liveChatListMode) return isAdminUser() ? "담당할 문의방을 선택해주세요." : "문의방을 선택해주세요.";
  if (state.liveChatExited) return "문의방에서 나갔습니다.";
  if (state.liveChatConnected) return "상담방에 연결되었습니다.";
  if (state.liveChatConnecting) return "상담방에 연결하는 중입니다.";
  return isAdminUser() ? "관리자는 문의방을 선택해 상담할 수 있습니다." : "로그인 회원은 상담사에게 문의할 수 있습니다.";
}

function updateLiveChatControls() {
  const isLiveMode = state.chatMode === "live";
  const isCustomer = state.liveChatRoom?.customerId === getCurrentUserId();
  const shouldShowLeaveButton = isLiveMode && isCustomer && !state.liveChatExited;
  const canCreateChatFromForm = canCreateLiveChatFromForm();
  const shouldShowNewInquirySubmit = isCustomerLiveChatList();
  $("#chatForm").classList.toggle("new-inquiry-mode", shouldShowNewInquirySubmit);
  $("#chatLeaveButton").classList.toggle("hidden", !shouldShowLeaveButton);
  $("#chatLeaveButton").disabled = !shouldShowLeaveButton || !state.liveChatConnected || state.liveChatConnecting || state.liveChatExited;
  $("#chatInput").disabled = shouldShowNewInquirySubmit || (isLiveMode && (state.liveChatCreating || (!canCreateChatFromForm && (state.liveChatListMode || state.liveChatExited))));
  $("#chatSendButton").textContent = shouldShowNewInquirySubmit ? "새 문의" : "전송";
  $("#chatSendButton").disabled = isLiveMode && (state.liveChatCreating || (!canCreateChatFromForm && (state.liveChatConnecting || state.liveChatListMode || state.liveChatExited)));
}

function isCustomerLiveChatList() {
  return state.chatMode === "live"
    && state.liveChatListMode
    && !isAdminUser();
}

function isEmptyCustomerLiveChatList() {
  return isCustomerLiveChatList() && !state.liveChatRooms.length;
}

function canCreateLiveChatFromForm() {
  return isCustomerLiveChatList()
    && !state.liveChatCreating
    && !state.liveChatConnecting;
}

async function startLiveChat() {
  if (state.liveChatConnecting || state.liveChatConnected) return;
  if (!state.token) {
    showToast("로그인 후 1:1 문의를 이용할 수 있습니다.");
    showView("auth");
    $("#chatStatusText").textContent = getLiveChatStatusText();
    return;
  }
  if (!window.SockJS || !window.Stomp) {
    addLiveMessage({ content: "실시간 채팅 스크립트를 불러오지 못했습니다. 네트워크 연결을 확인해주세요.", messageType: "SYSTEM" });
    return;
  }

  state.liveChatConnecting = true;
  updateLiveChatControls();
  $("#chatStatusText").textContent = getLiveChatStatusText();

  try {
    await ensureCurrentUser();
    if (!state.liveChatRoom) {
      await loadLiveChatRooms();
      return;
    }
    await loadLiveChatMessages();
    await connectLiveChat();
  } catch (error) {
    addLiveMessage({ content: error.message, messageType: "SYSTEM" });
  } finally {
    state.liveChatConnecting = false;
    $("#chatStatusText").textContent = getLiveChatStatusText();
    updateLiveChatControls();
  }
}

async function loadLiveChatMessages() {
  if (!state.liveChatRoom?.roomId) return;
  const messages = await request(`/api/chat/rooms/${state.liveChatRoom.roomId}/messages?size=50`);
  const recentMessages = isAdminUser()
    ? messages || []
    : (messages || []).filter((message) => (
        isVisibleCustomerLiveMessage(message)
        && (!message.createdAt || new Date(message.createdAt) >= oneMonthAgo())
      ));
  state.liveChatMessages = [...recentMessages].sort((a, b) => Number(a.messageId || 0) - Number(b.messageId || 0));
  state.lastLiveMessageId = state.liveChatMessages.reduce((max, message) => Math.max(max, Number(message.messageId || 0)), 0);
  renderChatMessages();
}

async function loadLiveChatRooms() {
  const rooms = await request("/api/chat/rooms?page=0&size=100");
  state.liveChatRooms = isAdminUser()
    ? sortAdminChatRooms(rooms.content || [])
    : sortCustomerChatRooms(dedupeCustomerChatRooms(rooms.content || []));
  state.liveChatListMode = true;
  state.liveChatMessages = [];
  renderChatMessages();
  $("#chatStatusText").textContent = getLiveChatStatusText();
  updateLiveChatControls();
}

function sortAdminChatRooms(rooms) {
  return [...rooms].sort((a, b) => {
    const aWaiting = !a.adminId;
    const bWaiting = !b.adminId;
    if (aWaiting !== bWaiting) return aWaiting ? -1 : 1;
    const aTime = new Date(a.createdAt).getTime();
    const bTime = new Date(b.createdAt).getTime();
    return aWaiting ? aTime - bTime : bTime - aTime;
  });
}

function sortCustomerChatRooms(rooms) {
  return [...rooms].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
}

function dedupeCustomerChatRooms(rooms) {
  const roomMap = new Map();
  rooms.forEach((room) => {
    const key = [
      room.name,
      room.status,
      room.adminId || "",
      formatDateTime(room.createdAt)
    ].join("|");
    if (!roomMap.has(key)) {
      roomMap.set(key, room);
    }
  });
  return [...roomMap.values()];
}

async function openLiveChatRoom(roomId) {
  const room = state.liveChatRooms.find((item) => String(item.roomId) === String(roomId));
  if (!room) return;
  disconnectLiveChat();
  state.liveChatRoom = room;
  state.liveChatListMode = false;
  state.liveChatExited = room.status === "COMPLETED" || isExitedLiveChatRoom(room);
  state.liveChatMessages = [];
  $("#chatStatusText").textContent = getLiveChatStatusText();
  renderChatMessages();
  if (state.liveChatExited) {
    await loadLiveChatMessages();
    $("#chatStatusText").textContent = getLiveChatStatusText();
    updateLiveChatControls();
    return;
  }
  await startLiveChat();
}

async function createLiveChatRoom(initialMessage = "") {
  if (isAdminUser() || state.liveChatCreating) return;
  try {
    state.liveChatCreating = true;
    state.liveChatExited = false;
    state.liveChatRoom = null;
    state.liveChatMessages = [];
    updateLiveChatControls();
    renderChatMessages();
    const room = await request("/api/chat/rooms/me", { method: "POST" });
    state.liveChatRooms = [room, ...state.liveChatRooms];
    await openLiveChatRoom(room.roomId);
    if (initialMessage.trim()) {
      sendLiveChatMessage(initialMessage.trim());
    }
  } catch (error) {
    showToast(error.message);
  } finally {
    state.liveChatCreating = false;
    updateLiveChatControls();
    renderChatMessages();
  }
}

function connectLiveChat() {
  return new Promise((resolve, reject) => {
    const socket = new SockJS("/ws");
    const client = Stomp.over(socket);
    client.debug = null;
    state.stompClient = client;

    client.connect(
      { Authorization: `Bearer ${state.token}` },
      () => {
        const roomId = state.liveChatRoom.roomId;
        state.liveChatSubscription = client.subscribe(`/sub/chat/${roomId}`, (frame) => {
          addLiveMessage(JSON.parse(frame.body));
        });
        client.send("/pub/chat.enter", {}, JSON.stringify({ roomId }));
        state.liveChatConnected = true;
        updateLiveChatControls();
        resolve();
      },
      () => {
        state.liveChatConnected = false;
        updateLiveChatControls();
        reject(new Error("실시간 상담 연결에 실패했습니다."));
      }
    );
  });
}

function sendLiveChatMessage(message) {
  if (state.liveChatListMode) {
    if (canCreateLiveChatFromForm()) {
      $("#chatInput").value = "";
      createLiveChatRoom(message);
      return;
    }
    showToast("문의방을 선택한 뒤 메시지를 보낼 수 있습니다.");
    return;
  }
  if (state.liveChatExited) {
    showToast("종료된 문의방에는 메시지를 보낼 수 없습니다.");
    return;
  }
  if (!state.liveChatConnected || !state.stompClient?.connected || !state.liveChatRoom?.roomId) {
    showToast("상담방 연결 후 메시지를 보낼 수 있습니다.");
    startLiveChat();
    return;
  }
  state.stompClient.send("/pub/chat.send", {}, JSON.stringify({
    roomId: state.liveChatRoom.roomId,
    content: message
  }));
  $("#chatInput").value = "";
}

function leaveLiveChatRoom() {
  if (!state.liveChatConnected || !state.stompClient?.connected || !state.liveChatRoom?.roomId) {
    showToast("현재 연결된 문의방이 없습니다.");
    return;
  }

  const roomId = state.liveChatRoom.roomId;
  $("#chatLeaveButton").disabled = true;
  $("#chatSendButton").disabled = true;
  $("#chatStatusText").textContent = "문의방에서 나가는 중입니다.";

  state.stompClient.send("/pub/chat.leave", {}, JSON.stringify({ roomId }));
  window.setTimeout(async () => {
    rememberExitedLiveChatRoom(state.liveChatRoom);
    if (state.liveChatRoom) {
      state.liveChatRoom.status = "COMPLETED";
    }
    state.liveChatRooms = state.liveChatRooms.map((room) => (
      String(room.roomId) === String(roomId) ? { ...room, status: "COMPLETED" } : room
    ));
    disconnectLiveChat();
    state.liveChatRoom = null;
    state.liveChatExited = false;
    state.liveChatListMode = true;
    state.liveChatMessages = [];
    renderChatMessages();
    $("#chatStatusText").textContent = getLiveChatStatusText();
    updateLiveChatControls();
    showToast("1:1 문의방에서 나갔습니다.");
    try {
      await loadLiveChatRooms();
    } catch (error) {
      showToast(error.message);
    }
  }, 300);
}

function disconnectLiveChat() {
  if (state.liveChatSubscription) {
    state.liveChatSubscription.unsubscribe();
    state.liveChatSubscription = null;
  }
  if (state.stompClient?.connected) {
    state.stompClient.disconnect(() => {});
  }
  state.stompClient = null;
  state.liveChatConnected = false;
  state.liveChatConnecting = false;
  updateLiveChatControls();
}

function showView(name) {
  document.querySelectorAll(".view").forEach((view) => view.classList.remove("active"));
  $(`#${name}View`).classList.add("active");
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function productType(product) {
  const categoryTypes = {
    1: "smartphone",
    2: "tablet",
    3: "laptop-icon",
    4: "desktop",
    5: "monitor",
    6: "input",
    7: "audio",
    8: "speaker",
    9: "watch",
    10: "camera",
    11: "console",
    12: "storage",
    13: "network",
    14: "charger",
    15: "pcpart",
    16: "home-appliance",
    17: "kitchen",
    18: "seasonal",
    19: "accessory",
    20: "etc-device"
  };
  if (categoryTypes[product.categoryId]) {
    return categoryTypes[product.categoryId];
  }

  const text = `${product.name} ${product.categoryName || ""}`.toLowerCase();
  if (text.includes("watch") || text.includes("워치")) return "watch";
  if (text.includes("sound") || text.includes("buds") || text.includes("음향")) return "audio";
  if (text.includes("book") || text.includes("tablet") || text.includes("노트북") || text.includes("태블릿")) return "tablet";
  return "";
}

function productCard(product) {
  return `
    <article class="product">
      <button data-product-id="${product.id}">
        <div class="thumb"><div class="gadget ${productType(product)}"></div></div>
        <strong>${escapeHtml(product.name)}</strong>
        <span>${escapeHtml(product.categoryName || "전자제품")}</span>
        <div class="price">${money(product.price)}</div>
      </button>
    </article>
  `;
}

function bestProductCard(product, rank) {
  return `
    <article class="product best-product">
      <span class="rank-badge">${rank}</span>
      <button data-product-id="${product.id}">
        <div class="thumb"><div class="gadget ${productType(product)}"></div></div>
        <strong>${escapeHtml(product.name)}</strong>
        <span>${escapeHtml(product.categoryName || "전자제품")}</span>
        <div class="price">${money(product.price)}</div>
      </button>
    </article>
  `;
}

function renderProductList(selector, products, emptyMessage) {
  const grid = $(selector);
  if (!products.length) {
    grid.innerHTML = `<div class="empty">${emptyMessage}</div>`;
    return;
  }
  grid.innerHTML = products.map(productCard).join("");
}

function renderProducts(products) {
  renderProductList("#productGrid", products, "표시할 상품이 없습니다.");
}

function renderSearchProducts(products) {
  renderProductList("#searchResultGrid", products, "검색 결과가 없습니다.");
}

function renderPagination(selector, page, totalPages, totalElements, previousAction, nextAction) {
  const container = $(selector);
  if (!totalElements) {
    container.innerHTML = "";
    return;
  }

  container.innerHTML = `
    <button class="secondary" type="button" data-page-action="${previousAction}" ${page <= 0 ? "disabled" : ""}>이전</button>
    <span>${page + 1} / ${totalPages || 1} 페이지 · 총 ${totalElements}개</span>
    <button class="secondary" type="button" data-page-action="${nextAction}" ${page + 1 >= totalPages ? "disabled" : ""}>다음</button>
  `;
}

function renderBestProducts(products) {
  const grid = $("#bestProductGrid");
  const bestProducts = products.slice(0, 10);
  if (!bestProducts.length) {
    grid.className = "best-product-grid";
    grid.innerHTML = `<div class="empty">베스트 상품이 없습니다.</div>`;
    return;
  }

  if (state.bestExpanded) {
    grid.className = "best-product-grid expanded";
    grid.innerHTML = bestProducts.map((product, index) => bestProductCard(product, index + 1)).join("");
    $("#bestToggleButton").textContent = "흐르는 상품 보기";
    return;
  }

  const carouselProducts = [...bestProducts, ...bestProducts];
  grid.className = "best-product-grid carousel";
  grid.innerHTML = `
    <div class="best-track">
      ${carouselProducts.map((product, index) => bestProductCard(product, (index % bestProducts.length) + 1)).join("")}
    </div>
  `;
  $("#bestToggleButton").textContent = "베스트 상품 보기";
}

function renderCategories() {
  $("#categoryStrip").innerHTML = categories.map((category) => `
    <button class="category ${category.id === state.productCategoryId ? "active" : ""}" data-category-id="${category.id}" type="button">
      ${escapeHtml(category.name)}<b>${escapeHtml(category.description)}</b>
    </button>
  `).join("");
}

function sortSampleProducts(products, sort) {
  const sorted = [...products];
  if (sort === "PRICE_ASC") {
    return sorted.sort((a, b) => a.price - b.price);
  }
  if (sort === "PRICE_DESC") {
    return sorted.sort((a, b) => b.price - a.price);
  }
  return sorted.reverse();
}

function getSampleProducts(keyword, sort, categoryId) {
  const filteredByCategory = categoryId
    ? samples.filter((product) => String(product.categoryId) === String(categoryId))
    : samples;
  const filtered = keyword
    ? filteredByCategory.filter((product) => product.name.includes(keyword) || product.categoryName.includes(keyword))
    : filteredByCategory;
  return sortSampleProducts(filtered, sort);
}

function updateSortButtons() {
  document.querySelectorAll("[data-product-sort]").forEach((button) => {
    button.classList.toggle("active", button.dataset.productSort === state.productSort);
  });
}

function updateSearchSortButtons() {
  document.querySelectorAll("[data-search-sort]").forEach((button) => {
    button.classList.toggle("active", button.dataset.searchSort === state.searchSort);
  });
}

function findCategoryIdByKeyword(keyword) {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) return "";
  const category = categories.find((item) => {
    if (!item.id) return false;
    const normalizedName = item.name.toLowerCase();
    return normalizedName.includes(normalizedKeyword) || normalizedKeyword.includes(normalizedName);
  });
  return category?.id || "";
}

function getCategoryName(categoryId) {
  return categories.find((category) => category.id === String(categoryId))?.name || "";
}

function updateSearchHeader() {
  const label = state.searchLabel || state.searchKeyword || getCategoryName(state.searchCategoryId) || "전체";
  $("#searchTitle").textContent = `"${label}" 검색 결과`;
  $("#searchMeta").textContent = `${state.searchTotalElements}개의 상품을 찾았습니다.`;
}

async function loadProducts(keyword = state.productKeyword, sort = state.productSort, categoryId = state.productCategoryId, page = 0) {
  state.productKeyword = keyword;
  state.productSort = sort;
  state.productCategoryId = categoryId;
  state.productPage = page;
  updateSortButtons();
  renderCategories();
  const sampleProducts = getSampleProducts(keyword, sort, categoryId);
  try {
    const query = new URLSearchParams({ page: String(page), size: "12", sort });
    if (keyword) query.set("keyword", keyword);
    if (categoryId) query.set("categoryId", categoryId);
    const data = await request(`/api/v1/products?${query.toString()}`);
    state.products = data.content || [];
    state.productPage = data.number || 0;
    state.productTotalPages = data.totalPages || 0;
    state.productTotalElements = data.totalElements || 0;
    renderProducts(state.products);
  } catch (error) {
    state.products = sampleProducts;
    state.productPage = 0;
    state.productTotalPages = sampleProducts.length ? 1 : 0;
    state.productTotalElements = sampleProducts.length;
    renderProducts(state.products);
    showToast("백엔드 상품 응답이 없어 샘플 상품을 보여드립니다.");
  }
  renderPagination(
    "#productPagination",
    state.productPage,
    state.productTotalPages,
    state.productTotalElements,
    "products-prev",
    "products-next"
  );
}

async function loadSearchResults(keyword, sort = state.searchSort, categoryId = state.searchCategoryId, label = "", page = 0) {
  state.searchKeyword = keyword;
  state.searchCategoryId = categoryId;
  state.searchLabel = label;
  state.searchSort = sort;
  state.searchPage = page;
  updateSearchSortButtons();
  showView("search");

  const sampleProducts = getSampleProducts(keyword, sort, categoryId);
  try {
    const query = new URLSearchParams({ page: String(page), size: "12", sort });
    if (keyword) query.set("keyword", keyword);
    if (categoryId) query.set("categoryId", categoryId);
    const data = await request(`/api/v1/products?${query.toString()}`);
    state.searchProducts = data.content || [];
    state.searchPage = data.number || 0;
    state.searchTotalPages = data.totalPages || 0;
    state.searchTotalElements = data.totalElements || 0;
  } catch (error) {
    state.searchProducts = sampleProducts;
    state.searchPage = 0;
    state.searchTotalPages = sampleProducts.length ? 1 : 0;
    state.searchTotalElements = sampleProducts.length;
    showToast("백엔드 상품 응답이 없어 샘플 상품을 보여드립니다.");
  }
  renderSearchProducts(state.searchProducts);
  updateSearchHeader();
  renderPagination(
    "#searchPagination",
    state.searchPage,
    state.searchTotalPages,
    state.searchTotalElements,
    "search-prev",
    "search-next"
  );
}

async function loadBestProducts() {
  const sampleProducts = getSampleProducts("", "PRICE_DESC", "").slice(0, 10);
  try {
    const query = new URLSearchParams({ page: "0", size: "10", sort: "PRICE_DESC" });
    const data = await request(`/api/v1/products?${query.toString()}`);
    state.bestProducts = (data.content || []).length ? data.content : sampleProducts;
  } catch (error) {
    state.bestProducts = sampleProducts;
  }
  renderBestProducts(state.bestProducts);
}

async function openProduct(productId) {
  const fallback =
    state.products.find((product) => String(product.id) === String(productId))
    || state.searchProducts.find((product) => String(product.id) === String(productId))
    || state.bestProducts.find((product) => String(product.id) === String(productId))
    || samples[0];
  try {
    state.currentProduct = await request(`/api/products/${productId}`);
  } catch (error) {
    state.currentProduct = fallback;
  }
  state.quantity = 1;
  renderDetail();
  showView("detail");
}

function renderDetail() {
  const product = state.currentProduct;
  $("#breadcrumbs").textContent = `홈 / ${product.categoryName || "상품"} / ${product.name}`;
  $("#detailName").textContent = product.name;
  $("#detailPrice").textContent = money(product.price);
  $("#detailDescription").textContent = product.description || "상세 설명은 백엔드 응답에 포함되면 자동으로 표시됩니다.";
  $("#detailMeta").textContent = "리뷰 1,284개";
  $("#detailDevice").className = `big-device ${productType(product)}`;
  $("#quantity").textContent = state.quantity;
  $("#totalPrice").textContent = money(product.price * state.quantity);
}

function openCartConfirm() {
  $("#cartConfirm").classList.remove("hidden");
  $("#cartConfirmGoButton").focus();
}

function closeCartConfirm() {
  $("#cartConfirm").classList.add("hidden");
  $("#addCartButton").focus();
}

function goCartFromConfirm() {
  closeCartConfirm();
  showView("cart");
  loadCart();
}

async function addCart() {
  if (!state.token) {
    showToast("로그인 후 장바구니에 담을 수 있습니다.");
    showView("auth");
    return;
  }
  try {
    await request("/api/carts/items", {
      method: "POST",
      body: JSON.stringify({ productId: state.currentProduct.id, quantity: state.quantity })
    });
    openCartConfirm();
  } catch (error) {
    showToast(error.message);
  }
}

function prepareDirectCheckout() {
  const product = state.currentProduct;
  state.checkoutItems = [{
    productId: product.id,
    productName: product.name,
    quantity: state.quantity,
    unitPrice: product.price,
    lineAmount: product.price * state.quantity
  }];
  state.checkoutOrder = null;
  state.checkoutSource = "direct";
  state.checkoutCartItemIds = [];
  state.selectedUserCouponId = null;
  renderCheckout();
  loadCheckoutCoupons();
  showView("checkout");
}

async function loadCart() {
  if (!state.token) {
    $("#cartList").innerHTML = `<div class="empty">로그인하면 내 장바구니를 확인할 수 있습니다.</div>`;
    return;
  }
  try {
    const cart = await request("/api/carts");
    renderCart(cart.items || [], cart.totalAmount || 0);
  } catch (error) {
    $("#cartList").innerHTML = `<div class="empty">${error.message}</div>`;
  }
}

function renderCart(items, totalAmount) {
  if (!items.length) {
    $("#cartList").innerHTML = `<div class="empty">장바구니가 비어 있습니다.</div>`;
  } else {
    $("#cartList").innerHTML = items.map((item) => `
      <article class="cart-item">
        <div class="mini-thumb"></div>
        <div>
          <div class="item-title">${escapeHtml(item.productName)}</div>
          <div class="item-meta">수량 ${item.quantity} · 재고 ${item.stock ?? "-"}개</div>
        </div>
        <strong>${money(item.lineAmount)}</strong>
      </article>
    `).join("");
  }
  $("#cartSubtotal").textContent = money(totalAmount);
  $("#cartTotal").textContent = money(totalAmount);
}

async function orderCart() {
  if (!state.token) {
    showToast("로그인 후 주문할 수 있습니다.");
    showView("auth");
    return;
  }
  try {
    const cart = await request("/api/carts");
    const ids = (cart.items || []).map((item) => item.cartItemId);
    if (!ids.length) {
      showToast("주문할 장바구니 상품이 없습니다.");
      return;
    }
    state.checkoutItems = cart.items || [];
    state.checkoutCartItemIds = ids;
    state.checkoutOrder = null;
    state.checkoutSource = "cart";
    state.selectedUserCouponId = null;
    renderCheckout(cart.totalAmount || 0);
    loadCheckoutCoupons();
    showView("checkout");
  } catch (error) {
    showToast(error.message);
  }
}

function renderCheckout(total) {
  const items = state.checkoutItems;
  const subtotal = total ?? items.reduce((sum, item) => sum + (item.lineAmount || 0), 0);
  const discountAmount = calculateSelectedCouponDiscount($("#checkoutCouponSelect")?.selectedOptions[0], subtotal);
  const paymentAmount = Math.max(subtotal - discountAmount, 0);
  state.checkoutAmount = subtotal;
  $("#checkoutList").innerHTML = items.map((item) => `
    <article class="checkout-item">
      <div class="mini-thumb"></div>
      <div>
        <div class="item-title">${escapeHtml(item.productName)}</div>
        <div class="item-meta">수량 ${item.quantity}</div>
      </div>
      <strong>${money(item.lineAmount)}</strong>
    </article>
  `).join("") || `<div class="empty">주문할 상품이 없습니다.</div>`;
  renderCheckoutSummary(subtotal, discountAmount, paymentAmount);
}

function renderCheckoutSummary(subtotal, discountAmount, paymentAmount) {
  $("#checkoutSubtotal").textContent = money(subtotal);
  $("#checkoutDiscount").textContent = `-${money(discountAmount)}`;
  $("#checkoutTotal").textContent = money(paymentAmount);
}

async function loadCheckoutCoupons() {
  const select = $("#checkoutCouponSelect");
  if (!select) return;

  if (!state.token) {
    select.innerHTML = `<option value="">로그인 후 쿠폰을 선택할 수 있습니다.</option>`;
    select.disabled = true;
    return;
  }

  select.disabled = true;
  select.innerHTML = `<option value="">쿠폰을 불러오는 중입니다.</option>`;

  try {
    const page = await request("/api/users/me/coupons?page=0&size=20");
    const coupons = page.content || [];
    const eventMap = await loadCouponEventsById();
    select.innerHTML = [
      `<option value="">쿠폰 사용 안 함</option>`,
      ...coupons.map((coupon) => {
        const event = eventMap.get(coupon.couponEventId);
        const label = event
          ? `${event.name} (${formatDiscount(event)}, 만료 ${formatDate(coupon.expiredAt)})`
          : `쿠폰 #${coupon.id} (만료 ${formatDate(coupon.expiredAt)})`;
        return `
          <option
            value="${coupon.id}"
            data-discount-type="${escapeHtml(event?.discountType || "")}"
            data-discount-amount="${event?.discountAmount ?? 0}"
          >${escapeHtml(label)}</option>
        `;
      })
    ].join("");
    select.value = state.selectedUserCouponId ? String(state.selectedUserCouponId) : "";
    select.disabled = false;
    renderCheckout();
  } catch (error) {
    select.innerHTML = `<option value="">${escapeHtml(error.message)}</option>`;
    select.disabled = true;
  }
}

function selectCheckoutCoupon(event) {
  const select = event.currentTarget;
  const selectedOption = select.selectedOptions[0];
  const discountAmount = calculateSelectedCouponDiscount(selectedOption, state.checkoutAmount);
  if (discountAmount > state.checkoutAmount) {
    showToast("상품 금액보다 큰 쿠폰은 적용할 수 없습니다.");
    select.value = "";
    state.selectedUserCouponId = null;
    state.checkoutOrder = null;
    renderCheckout();
    return;
  }

  state.selectedUserCouponId = select.value ? Number(select.value) : null;
  state.checkoutOrder = null;
  renderCheckout();
}

function calculateSelectedCouponDiscount(option, baseAmount = state.checkoutAmount) {
  if (!option?.value) return 0;
  const discountAmount = Number(option.dataset.discountAmount || 0);
  if (option.dataset.discountType === "PERCENT") {
    return Math.floor(baseAmount * discountAmount / 100);
  }
  return discountAmount;
}

function backToCart() {
  state.checkoutOrder = null;
  state.checkoutSource = null;
  state.checkoutCartItemIds = [];
  state.selectedUserCouponId = null;
  showView("cart");
  loadCart();
}

async function createCheckoutOrderIfNeeded() {
  if (state.checkoutOrder?.paymentId) {
    return state.checkoutOrder;
  }

  if (state.checkoutSource === "cart") {
    if (!state.checkoutCartItemIds.length) {
      throw new Error("주문할 장바구니 상품이 없습니다.");
    }

    const order = await request("/api/orders/carts", {
      method: "POST",
      body: JSON.stringify({
        cartItemIds: state.checkoutCartItemIds,
        userCouponId: state.selectedUserCouponId
      })
    });
    state.checkoutOrder = order;
    state.checkoutItems = order.items || state.checkoutItems;
    renderCheckoutSummary(order.totalProductAmount, order.usedCouponAmount, order.paymentAmount);
    return order;
  }

  const item = state.checkoutItems[0];
  if (!item) {
    throw new Error("주문할 상품이 없습니다.");
  }

  const order = await request("/api/orders/direct", {
    method: "POST",
    body: JSON.stringify({
      productId: item.productId,
      quantity: item.quantity,
      userCouponId: state.selectedUserCouponId
    })
  });
  state.checkoutOrder = order;
  state.checkoutItems = order.items || state.checkoutItems;
  renderCheckoutSummary(order.totalProductAmount, order.usedCouponAmount, order.paymentAmount);
  return order;
}

function getCheckoutOrderName(order) {
  const items = order.items || state.checkoutItems;
  if (!items.length) return order.orderNumber || "삼조전자 주문";
  if (items.length === 1) return items[0].productName;
  return `${items[0].productName} 외 ${items.length - 1}개`;
}

function isFreeAmount(amount) {
  return Number(amount) === 0;
}

async function finishCheckoutPayment() {
  showToast("결제가 완료되었습니다.");
  state.checkoutOrder = null;
  state.checkoutSource = null;
  state.checkoutCartItemIds = [];
  state.selectedUserCouponId = null;
  await loadOrders(0);
  showView("account");
}

async function processPayment() {
  if (!state.token) {
    showToast("로그인 후 결제할 수 있습니다.");
    showView("auth");
    return;
  }

  const paymentButton = $("#paymentButton");
  const originalText = paymentButton.textContent;
  paymentButton.disabled = true;
  paymentButton.textContent = "결제창 준비 중";

  try {
    const order = await createCheckoutOrderIfNeeded();
    if (!order.paymentId) {
      throw new Error("결제 ID를 확인하지 못했습니다. 주문을 다시 생성해주세요.");
    }

    if (isFreeAmount(order.paymentAmount)) {
      paymentButton.textContent = "결제 완료 처리 중";
      await request(`/api/payments/${order.paymentId}/free-complete`, { method: "POST" });
      await finishCheckoutPayment();
      return;
    }

    const payment = await request(`/api/payments/${order.paymentId}/start`, { method: "POST" });

    if (isFreeAmount(payment.paymentAmount)) {
      paymentButton.textContent = "결제 완료 처리 중";
      await request(`/api/payments/${order.paymentId}/free-complete`, { method: "POST" });
      await finishCheckoutPayment();
      return;
    }

    if (!window.PortOne?.requestPayment) {
      throw new Error("포트원 결제 SDK를 불러오지 못했습니다.");
    }

    const config = await request("/api/config/portone");

    if (!config.storeId || !config.channelKey) {
      throw new Error("포트원 공개 설정이 비어 있습니다.");
    }
    if (!payment.portOnePaymentId) {
      throw new Error("포트원 결제 ID를 확인하지 못했습니다. 주문을 다시 생성해주세요.");
    }

    const user = await ensureCurrentUser();
    const customerId = user?.userId ?? getCurrentUserId();
    if (!customerId) {
      throw new Error("회원 ID를 확인하지 못했습니다. 다시 로그인한 뒤 결제해주세요.");
    }

    const paymentResult = await window.PortOne.requestPayment({
      storeId: config.storeId,
      channelKey: config.channelKey,
      paymentId: payment.portOnePaymentId,
      orderName: getCheckoutOrderName(order),
      totalAmount: payment.paymentAmount,
      currency: "CURRENCY_KRW",
      payMethod: "CARD",
      customer: {
        customerId: String(customerId),
        fullName: user.name,
        email: user.email,
        phoneNumber: user.phone
      }
    });

    if (paymentResult?.code) {
      showToast(paymentResult.message || "결제가 취소되었습니다.");
      return;
    }

    await request("/api/payments/confirm", {
      method: "POST",
      body: JSON.stringify({
        paymentId: payment.paymentId,
        portOnePaymentId: paymentResult?.paymentId || payment.portOnePaymentId
      })
    });

    await finishCheckoutPayment();
  } catch (error) {
    showToast(error.message);
  } finally {
    paymentButton.disabled = false;
    paymentButton.textContent = originalText;
  }
}

async function submitAuth(event) {
  event.preventDefault();
  const mode = $("#authSubmit").dataset.mode || "login";
  const form = new FormData(event.currentTarget);
  const payload = Object.fromEntries(form.entries());
  const path = mode === "signup" ? "/api/auth/signup" : "/api/auth/login";
  try {
    const data = await request(path, { method: "POST", body: JSON.stringify(payload) });
    if (mode === "signup") {
      showToast("회원가입이 완료되었습니다. 이제 로그인해주세요.");
      setAuthMode("login");
      return;
    }
    state.token = data.accessToken;
    localStorage.setItem("accessToken", state.token);
    await ensureCurrentUser();
    updateAuthButton();
    if (state.chatMode === "live") {
      startLiveChat();
    }
    showToast("로그인되었습니다.");
    showView("home");
  } catch (error) {
    showToast(error.message);
  }
}

function setAuthMode(mode) {
  document.querySelectorAll(".tab").forEach((tab) => {
    tab.classList.toggle("active", tab.dataset.authMode === mode);
  });
  document.querySelectorAll(".signup-only").forEach((field) => {
    field.classList.toggle("hidden", mode !== "signup");
  });
  $("#authSubmit").textContent = mode === "signup" ? "회원가입" : "로그인";
  $("#authSubmit").dataset.mode = mode;
}

function updateAuthButton() {
  $("#authButton").textContent = state.token ? "내 정보" : "로그인";
  $("#authButton").dataset.view = state.token ? "account" : "auth";
  $("#liveChatTab").classList.toggle("locked", !state.token);
  $("#liveChatTab").setAttribute("aria-disabled", String(!state.token));
  updateLiveChatControls();
  document.querySelectorAll("[data-auth-required]").forEach((button) => {
    button.classList.toggle("hidden", !state.token);
    button.disabled = !state.token;
    button.setAttribute("aria-disabled", String(!state.token));
  });
}

function requireAuth(view) {
  if (state.token || !["account", "cart", "checkout"].includes(view)) {
    return true;
  }
  showToast("로그인 후 이용할 수 있습니다.");
  showView("auth");
  return false;
}

async function loadProfile() {
  if (!state.token) {
    $("#profileName").textContent = "회원님";
    $("#profileContact").innerHTML = `<span>로그인하면 내 정보를 확인할 수 있습니다.</span>`;
    $("#couponCount").textContent = "0장";
    $("#orderList").innerHTML = `<div class="empty">로그인이 필요합니다.</div>`;
    $("#orderPagination").innerHTML = "";
    return;
  }
  try {
    const profile = await request("/api/users/me");
    state.currentUser = profile;
    $("#profileName").textContent = `${profile.name}님`;
    $("#profileContact").innerHTML = `
      <span>${escapeHtml(profile.email)}</span>
      <span>${escapeHtml(profile.phone || "전화번호 정보가 없습니다.")}</span>
    `;
    loadCoupons(false);
  } catch (error) {
    showToast(error.message);
  }
}

async function loadCoupons(openPopover = true) {
  const tile = $("#couponTile");
  const popover = $("#couponPopover");
  if (!state.token) {
    $("#couponCount").textContent = "0장";
    popover.innerHTML = `<span class="coupon-message">로그인하면 보유 쿠폰을 확인할 수 있습니다.</span>`;
    tile.classList.toggle("open", openPopover);
    tile.setAttribute("aria-expanded", String(openPopover));
    return;
  }

  try {
    const page = await request("/api/users/me/coupons?page=0&size=10");
    const coupons = page.content || [];
    const eventMap = await loadCouponEventsById();
    $("#couponCount").textContent = `${page.totalElements ?? coupons.length}장`;
    popover.innerHTML = coupons.length
      ? coupons.map((coupon) => {
          const event = eventMap.get(coupon.couponEventId);
          return `
            <span class="coupon-item">
              <strong>${escapeHtml(event?.name || `쿠폰 #${coupon.id}`)}</strong>
              <span>${escapeHtml(formatDiscount(event))}</span>
              <span>만료일 ${escapeHtml(formatDateTime(coupon.expiredAt))}</span>
            </span>
          `;
        }).join("")
      : `<span class="coupon-message">${NO_COUPON_MESSAGE}</span>`;
  } catch (error) {
    popover.innerHTML = `<span class="coupon-message">${escapeHtml(error.message)}</span>`;
  }

  tile.classList.toggle("open", openPopover);
  tile.setAttribute("aria-expanded", String(openPopover));
}

async function loadCouponEventsById() {
  try {
    const page = await request("/api/coupon-events?page=0&size=100");
    return new Map((page.content || []).map((event) => [event.id, event]));
  } catch (error) {
    return new Map();
  }
}

async function loadOrders(pageNumber = state.orderPage) {
  clearOrderDetailPanel();
  if (!state.token) {
    $("#orderList").innerHTML = `<div class="empty">로그인하면 주문 내역을 확인할 수 있습니다.</div>`;
    $("#orderPagination").innerHTML = "";
    return;
  }
  try {
    const page = await request(`/api/orders?page=${pageNumber}&size=10`);
    const orders = page.content || [];
    const orderSummaries = await Promise.all(orders.map(loadOrderSummary));
    state.orderPage = page.page || 0;
    state.orderTotalPages = page.totalPages || 0;
    state.orderTotalElements = page.totalElements || orders.length;
    $("#orderCount").textContent = `${state.orderTotalElements}건`;
    $("#orderList").innerHTML = orderSummaries.map((order) => `
      <button class="order-item" type="button" data-order-id="${order.orderId}">
        <div class="mini-thumb"></div>
        <div>
          <div class="item-title">${escapeHtml(order.displayName)}</div>
          <div class="item-meta">${formatDate(order.createdAt)} · ${money(order.paymentAmount)}</div>
        </div>
        <span class="badge">${formatOrderStatus(order.status)}</span>
      </button>
    `).join("") || `<div class="empty">주문 내역이 없습니다.</div>`;
    renderPagination(
      "#orderPagination",
      state.orderPage,
      state.orderTotalPages,
      state.orderTotalElements,
      "orders-prev",
      "orders-next"
    );
  } catch (error) {
    $("#orderList").innerHTML = `<div class="empty">${escapeHtml(error.message)}</div>`;
    $("#orderPagination").innerHTML = "";
  }
}

async function loadOrderSummary(order) {
  try {
    const detail = await request(`/api/orders/${order.orderId}`);
    return {
      ...order,
      displayName: getOrderDisplayName(detail)
    };
  } catch (error) {
    return {
      ...order,
      displayName: order.orderNumber
    };
  }
}

function getOrderDisplayName(order) {
  const items = order.items || [];
  if (!items.length) {
    return order.orderNumber;
  }
  if (items.length === 1) {
    return items[0].productName;
  }
  return `${items[0].productName} 외 ${items.length - 1}개`;
}

async function openOrderDetail(orderId) {
  const openedPanel = $("#orderDetailPanel");
  if (openedPanel?.dataset.openOrderId === orderId) {
    clearOrderDetailPanel();
    return;
  }

  clearOrderDetailPanel();
  const orderButton = document.querySelector(`[data-order-id="${CSS.escape(orderId)}"]`);
  const panel = document.createElement("div");
  panel.className = "order-detail-panel";
  panel.id = "orderDetailPanel";
  panel.dataset.openOrderId = orderId;
  panel.setAttribute("aria-live", "polite");
  panel.innerHTML = `<div class="empty">주문 상세를 불러오는 중입니다.</div>`;
  (orderButton || $("#orderList")).insertAdjacentElement(orderButton ? "afterend" : "beforeend", panel);

  try {
    const order = await request(`/api/orders/${orderId}`);
    renderOrderDetail(order);
  } catch (error) {
    panel.innerHTML = `<div class="empty">${escapeHtml(error.message)}</div>`;
  }
}

function renderOrderDetail(order) {
  const panel = $("#orderDetailPanel");
  if (!panel) return;
  const canCancel = order.status === "READY";
  const cancelMessage = getOrderCancelMessage(order.status);
  panel.innerHTML = `
    <div class="order-detail-head">
      <div>
        <h3>주문 상세</h3>
        <div class="item-meta">${escapeHtml(order.orderNumber)} · ${escapeHtml(formatDateTime(order.createdAt))}</div>
      </div>
      <span class="badge">${formatOrderStatus(order.status)}</span>
    </div>
    <div class="order-detail-items">
      ${(order.items || []).map((item) => `
        <div class="order-detail-row">
          <div>
            <div class="item-title">${escapeHtml(item.productName)}</div>
            <div class="item-meta">수량 ${item.quantity} · 단가 ${money(item.unitPrice)}</div>
          </div>
          <strong>${money(item.lineAmount)}</strong>
        </div>
      `).join("") || `<div class="empty">주문 상품이 없습니다.</div>`}
    </div>
    <div class="order-detail-total">
      <div class="pay-row"><span>상품 금액</span><b>${money(order.totalProductAmount)}</b></div>
      <div class="pay-row"><span>할인 금액</span><b>-${money(order.usedCouponAmount)}</b></div>
      <div class="pay-row total"><span>결제 금액</span><b>${money(order.paymentAmount)}</b></div>
    </div>
    ${canCancel
      ? `<button class="secondary" id="cancelOrderButton" data-cancel-order-id="${order.orderId}">주문 취소</button>`
      : cancelMessage ? `<div class="order-cancel-note">${escapeHtml(cancelMessage)}</div>` : ""}
  `;
}

async function cancelOrder(orderId) {
  try {
    await request(`/api/orders/${orderId}/cancel`, { method: "POST" });
    showToast("주문이 취소되었습니다.");
    await loadOrders();
    await openOrderDetail(orderId);
  } catch (error) {
    showToast(error.message);
  }
}

function logout() {
  state.token = null;
  state.currentUser = null;
  disconnectLiveChat();
  state.liveChatRoom = null;
  state.liveChatMessages = [];
  state.liveChatRooms = [];
  state.liveChatListMode = false;
  state.orderPage = 0;
  state.orderTotalPages = 0;
  state.orderTotalElements = 0;
  localStorage.removeItem("accessToken");
  updateAuthButton();
  if (state.chatMode === "live") {
    setChatMode("ai");
  }
  showToast("로그아웃되었습니다.");
  showView("home");
}

document.addEventListener("click", (event) => {
  const viewButton = event.target.closest("[data-view]");
  if (viewButton) {
    const view = viewButton.dataset.view;
    if (!requireAuth(view)) {
      return;
    }
    showView(view);
    if (view === "products") loadProducts();
    if (view === "cart") loadCart();
    if (view === "account") {
      loadProfile();
      loadOrders(0);
    }
  }

  const productButton = event.target.closest("[data-product-id]");
  if (productButton) {
    openProduct(productButton.dataset.productId);
  }

  const categoryButton = event.target.closest("[data-category-id]");
  if (categoryButton) {
    $("#searchInput").value = "";
    loadProducts("", state.productSort, categoryButton.dataset.categoryId, 0);
  }

  const pageButton = event.target.closest("[data-page-action]");
  if (pageButton) {
    const action = pageButton.dataset.pageAction;
    if (action === "products-prev") {
      loadProducts(state.productKeyword, state.productSort, state.productCategoryId, Math.max(0, state.productPage - 1));
    }
    if (action === "products-next") {
      loadProducts(state.productKeyword, state.productSort, state.productCategoryId, state.productPage + 1);
    }
    if (action === "search-prev") {
      loadSearchResults(state.searchKeyword, state.searchSort, state.searchCategoryId, state.searchLabel, Math.max(0, state.searchPage - 1));
    }
    if (action === "search-next") {
      loadSearchResults(state.searchKeyword, state.searchSort, state.searchCategoryId, state.searchLabel, state.searchPage + 1);
    }
    if (action === "orders-prev") {
      loadOrders(Math.max(0, state.orderPage - 1));
    }
    if (action === "orders-next") {
      loadOrders(state.orderPage + 1);
    }
  }

  const orderButton = event.target.closest("[data-order-id]");
  if (orderButton) {
    openOrderDetail(orderButton.dataset.orderId);
  }

  const cancelButton = event.target.closest("[data-cancel-order-id]");
  if (cancelButton) {
    cancelOrder(cancelButton.dataset.cancelOrderId);
  }

  const chatRoomButton = event.target.closest("[data-chat-room-id]");
  if (chatRoomButton) {
    openLiveChatRoom(chatRoomButton.dataset.chatRoomId);
  }

  if (!event.target.closest("#couponTile")) {
    $("#couponTile").classList.remove("open");
    $("#couponTile").setAttribute("aria-expanded", "false");
  }
});

$("#searchForm").addEventListener("submit", (event) => {
  event.preventDefault();
  const keyword = $("#searchInput").value.trim();
  if (!keyword) {
    showToast("검색어를 입력해주세요.");
    return;
  }
  const categoryId = findCategoryIdByKeyword(keyword);
  loadSearchResults(categoryId ? "" : keyword, state.searchSort, categoryId, keyword);
});

document.querySelectorAll("[data-product-sort]").forEach((button) => {
  button.addEventListener("click", () => loadProducts(state.productKeyword, button.dataset.productSort, state.productCategoryId, 0));
});

document.querySelectorAll("[data-search-sort]").forEach((button) => {
  button.addEventListener("click", () => loadSearchResults(
    state.searchKeyword,
    button.dataset.searchSort,
    state.searchCategoryId,
    state.searchLabel,
    0
  ));
});

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => setAuthMode(tab.dataset.authMode));
});

document.querySelectorAll("[data-chat-topic]").forEach((button) => {
  button.addEventListener("click", () => setChatTopic(button.dataset.chatTopic));
});

document.querySelectorAll("[data-chat-mode]").forEach((button) => {
  button.addEventListener("click", () => {
    if (button.dataset.chatMode === "live" && !state.token) {
      showToast("로그인 후 1:1 문의를 이용할 수 있습니다.");
      showView("auth");
      return;
    }
    setChatMode(button.dataset.chatMode);
  });
});

$("#increaseQty").addEventListener("click", () => {
  state.quantity += 1;
  renderDetail();
});

$("#decreaseQty").addEventListener("click", () => {
  state.quantity = Math.max(1, state.quantity - 1);
  renderDetail();
});

$("#bestToggleButton").addEventListener("click", () => {
  state.bestExpanded = !state.bestExpanded;
  renderBestProducts(state.bestProducts);
});
$("#addCartButton").addEventListener("click", addCart);
$("#directOrderButton").addEventListener("click", prepareDirectCheckout);
$("#authForm").addEventListener("submit", submitAuth);
$("#loadCartButton").addEventListener("click", loadCart);
$("#cartOrderButton").addEventListener("click", orderCart);
$("#cartConfirmGoButton").addEventListener("click", goCartFromConfirm);
$("#cartConfirmCloseButton").addEventListener("click", closeCartConfirm);
$("#cartConfirm").addEventListener("click", (event) => {
  if (event.target === event.currentTarget) {
    closeCartConfirm();
  }
});
$("#logoutButton").addEventListener("click", logout);
$("#checkoutCouponSelect").addEventListener("change", selectCheckoutCoupon);
$("#backToCartButton").addEventListener("click", backToCart);
$("#paymentButton").addEventListener("click", processPayment);
$("#chatToggleButton").addEventListener("click", () => {
  const isOpen = !$("#chatPanel").classList.contains("hidden");
  if (!isOpen) {
    setChatMode("ai");
  }
  setChatOpen(!isOpen);
});
$("#chatCloseButton").addEventListener("click", () => setChatOpen(false));
$("#chatLeaveButton").addEventListener("click", leaveLiveChatRoom);
$("#chatForm").addEventListener("submit", (event) => {
  event.preventDefault();
  if (state.chatMode === "live" && canCreateLiveChatFromForm()) {
    createLiveChatRoom();
    return;
  }
  const message = $("#chatInput").value.trim();
  if (!message) return;
  if (state.chatMode === "live") {
    sendLiveChatMessage(message);
    return;
  }
  sendChatMessage(message);
});
$("#couponTile").addEventListener("click", (event) => {
  event.stopPropagation();
  const isOpen = $("#couponTile").classList.contains("open");
  if (isOpen) {
    $("#couponTile").classList.remove("open");
    $("#couponTile").setAttribute("aria-expanded", "false");
    return;
  }
  loadCoupons(true);
});

updateAuthButton();
setAuthMode("login");
setChatTopic(state.chatTopic, { notify: false });
setChatMode("ai");
loadBestProducts();
loadProducts();
