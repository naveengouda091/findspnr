/* ==========================================================================
   CraftVault - Premium Minecraft Modpack Store Logic
   Interactive Catalog, Live HUD Simulator, Shopping Cart & Checkout
   ========================================================================== */

// Modpack Database
const modpacks = [
    {
        id: "findspnr-tactical",
        title: "FindSpnr Tactical Suite",
        category: "radar",
        price: 9.99,
        origPrice: 19.99,
        featured: true,
        icon: "🎯",
        desc: "Includes Fixed North-Up Radar, 3D See-Through ESP, Anti-Xray Dungeon Scanner, and Player Heading Arrow.",
        tags: ["1.21.4 & 26.2", "Fixed Radar", "3D Tracers", "Anti-Xray"]
    },
    {
        id: "apex-pvp-engine",
        title: "Apex PvP Ultra Engine",
        category: "pvp",
        price: 14.99,
        origPrice: 24.99,
        featured: false,
        icon: "⚔️",
        desc: "Zero-input lag optimization, custom crosshairs, auto-refill hotbar, and hit-box indicators for competitive server PvP.",
        tags: ["Zero Lag", "Hotbar Auto-Refill", "Hitbox Overlay"]
    },
    {
        id: "quantum-fps-overhaul",
        title: "Quantum FPS Overhaul",
        category: "fps",
        price: 7.99,
        origPrice: 15.99,
        featured: false,
        icon: "⚡",
        desc: "Sodium + Iris + Vulkan pipeline integration boosting framerates from 60 FPS up to 300+ FPS seamlessly.",
        tags: ["240+ FPS Boost", "Sodium Engine", "Vulkan Ready"]
    },
    {
        id: "economy-king-utility",
        title: "Economy King Utility Pack",
        category: "pvp",
        price: 11.99,
        origPrice: 19.99,
        featured: false,
        icon: "👑",
        desc: "Automated chest sorting, villager trade helper, waypoint synchronizer, and regional price tracker for server markets.",
        tags: ["Auto-Sorter", "Trade Helper", "Market Tracker"]
    },
    {
        id: "ultimate-operative-bundle",
        title: "Ultimate Operative All-In-One",
        category: "radar",
        price: 29.99,
        origPrice: 69.99,
        featured: true,
        icon: "💎",
        desc: "Complete access to all 4 modpacks + VIP Discord role + Lifetime priority auto-updates and instant single-click installer.",
        tags: ["Best Value", "All 4 Modpacks", "VIP Priority Updates"]
    }
];

// Cart State
let cart = [];
let discountApplied = false;

// DOM Elements
document.addEventListener("DOMContentLoaded", () => {
    initCatalog();
    initFiltersAndSearch();
    initCart();
    initSimulator();
    initFAQ();
    initNavbarScroll();
});

// Render Catalog Cards
function initCatalog(filter = "all", searchQuery = "") {
    const grid = document.getElementById("modpackGrid");
    grid.innerHTML = "";

    const filtered = modpacks.filter(pack => {
        const matchesTab = filter === "all" || pack.category === filter;
        const matchesSearch = pack.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                              pack.desc.toLowerCase().includes(searchQuery.toLowerCase());
        return matchesTab && matchesSearch;
    });

    if (filtered.length === 0) {
        grid.innerHTML = `<div class="no-results" style="grid-column: 1/-1; text-align:center; padding: 40px; color: var(--text-secondary);">No modpacks match your filter. Try another category!</div>`;
        return;
    }

    filtered.forEach(pack => {
        const card = document.createElement("div");
        card.className = "modpack-card";
        card.innerHTML = `
            ${pack.featured ? '<div class="card-badge-featured">BEST SELLER</div>' : ''}
            <div>
                <div class="card-icon-header">${pack.icon}</div>
                <h3 class="card-title">${pack.title}</h3>
                <p class="card-desc">${pack.desc}</p>
                <div class="card-tags">
                    ${pack.tags.map(tag => `<span class="tag-item">${tag}</span>`).join('')}
                </div>
            </div>
            <div class="card-footer">
                <div class="price-box">
                    <span class="price-amount">$${pack.price.toFixed(2)}</span>
                    <span class="price-orig">$${pack.origPrice.toFixed(2)}</span>
                </div>
                <button class="btn btn-primary" onclick="addToCart('${pack.id}')">
                    <span>Add to Cart</span>
                </button>
            </div>
        `;
        grid.appendChild(card);
    });
}

// Category Tabs & Search Event Listeners
function initFiltersAndSearch() {
    const tabBtns = document.querySelectorAll(".tab-btn");
    const searchInput = document.getElementById("searchInput");

    let currentFilter = "all";

    tabBtns.forEach(btn => {
        btn.addEventListener("click", () => {
            tabBtns.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            currentFilter = btn.getAttribute("data-filter");
            initCatalog(currentFilter, searchInput.value);
        });
    });

    searchInput.addEventListener("input", (e) => {
        initCatalog(currentFilter, e.target.value);
    });
}

// Cart Sidebar & Actions
function initCart() {
    const cartToggleBtn = document.getElementById("cartToggleBtn");
    const closeCartBtn = document.getElementById("closeCartBtn");
    const cartOverlay = document.getElementById("cartOverlay");
    const cartSidebar = document.getElementById("cartSidebar");
    const applyPromoBtn = document.getElementById("applyPromoBtn");
    const checkoutBtn = document.getElementById("checkoutBtn");

    cartToggleBtn.addEventListener("click", () => openCart());
    closeCartBtn.addEventListener("click", () => closeCart());
    cartOverlay.addEventListener("click", () => closeCart());

    applyPromoBtn.addEventListener("click", () => {
        const promoInput = document.getElementById("promoInput").value.trim().toUpperCase();
        if (promoInput === "CRAFT20") {
            discountApplied = true;
            showToast("🎉 Promo Code CRAFT20 Applied! 20% OFF");
            updateCartUI();
        } else {
            showToast("❌ Invalid Promo Code. Try 'CRAFT20'");
        }
    });

    checkoutBtn.addEventListener("click", () => {
        if (cart.length === 0) {
            showToast("Your cart is empty!");
            return;
        }
        showToast("🚀 Order Success! Check your downloads.");
        cart = [];
        discountApplied = false;
        updateCartUI();
        closeCart();
    });
}

function openCart() {
    document.getElementById("cartSidebar").classList.add("open");
    document.getElementById("cartOverlay").classList.add("open");
}

function closeCart() {
    document.getElementById("cartSidebar").classList.remove("open");
    document.getElementById("cartOverlay").classList.remove("open");
}

function addToCart(packId) {
    const pack = modpacks.find(p => p.id === packId);
    if (!pack) return;

    if (!cart.some(item => item.id === packId)) {
        cart.push(pack);
        showToast(`Added ${pack.title} to cart!`);
    } else {
        showToast(`Item already in cart!`);
    }

    updateCartUI();
    openCart();
}

function removeFromCart(packId) {
    cart = cart.filter(item => item.id !== packId);
    updateCartUI();
}

function updateCartUI() {
    const container = document.getElementById("cartItemsContainer");
    const cartBadge = document.getElementById("cartBadge");
    const subtotalEl = document.getElementById("subtotalPrice");
    const totalEl = document.getElementById("totalPrice");
    const discountRow = document.getElementById("discountRow");
    const discountEl = document.getElementById("discountPrice");

    cartBadge.textContent = cart.length;
    container.innerHTML = "";

    if (cart.length === 0) {
        container.innerHTML = `<p style="text-align:center; color: var(--text-muted); margin-top: 40px;">Your cart is empty.</p>`;
        subtotalEl.textContent = "$0.00";
        totalEl.textContent = "$0.00";
        discountRow.classList.add("hidden");
        return;
    }

    let subtotal = 0;
    cart.forEach(item => {
        subtotal += item.price;
        const itemEl = document.createElement("div");
        itemEl.className = "cart-item-card";
        itemEl.innerHTML = `
            <div class="cart-item-info">
                <h4>${item.title}</h4>
                <span>$${item.price.toFixed(2)}</span>
            </div>
            <button class="cart-item-remove" onclick="removeFromCart('${item.id}')">&times;</button>
        `;
        container.appendChild(itemEl);
    });

    let total = subtotal;
    if (discountApplied) {
        const discountAmount = subtotal * 0.20;
        total = subtotal - discountAmount;
        discountEl.textContent = `-$${discountAmount.toFixed(2)}`;
        discountRow.classList.remove("hidden");
    } else {
        discountRow.classList.add("hidden");
    }

    subtotalEl.textContent = `$${subtotal.toFixed(2)}`;
    totalEl.textContent = `$${total.toFixed(2)}`;
}

// Live HUD Demo Simulator
function initSimulator() {
    const openPreviewBtn = document.getElementById("openPreviewHeroBtn");
    const closePreviewBtn = document.getElementById("closePreviewBtn");
    const modal = document.getElementById("previewModal");

    const toggleRadarBtn = document.getElementById("toggleRadarSim");
    const toggleEspBtn = document.getElementById("toggleEspSim");
    const toggleTracerBtn = document.getElementById("toggleTracerSim");
    const yawRange = document.getElementById("yawRange");

    const simRadar = document.getElementById("simRadar");
    const sim3dEsp = document.getElementById("sim3dEsp");
    const tracerThread = document.querySelector(".tracer-thread");
    const playerArrow = document.getElementById("simPlayerArrow");

    openPreviewBtn.addEventListener("click", () => modal.classList.add("open"));
    closePreviewBtn.addEventListener("click", () => modal.classList.remove("open"));

    toggleRadarBtn.addEventListener("click", () => {
        toggleRadarBtn.classList.toggle("active");
        simRadar.style.display = toggleRadarBtn.classList.contains("active") ? "block" : "none";
    });

    toggleEspBtn.addEventListener("click", () => {
        toggleEspBtn.classList.toggle("active");
        sim3dEsp.style.display = toggleEspBtn.classList.contains("active") ? "block" : "none";
    });

    toggleTracerBtn.addEventListener("click", () => {
        toggleTracerBtn.classList.toggle("active");
        tracerThread.style.display = toggleTracerBtn.classList.contains("active") ? "block" : "none";
    });

    yawRange.addEventListener("input", (e) => {
        const angle = e.target.value;
        playerArrow.style.transform = `rotate(${angle}deg)`;
    });
}

// FAQ Accordion Toggle
function initFAQ() {
    const faqQuestions = document.querySelectorAll(".faq-question");
    faqQuestions.forEach(q => {
        q.addEventListener("click", () => {
            const item = q.parentElement;
            item.classList.toggle("active");
            const icon = q.querySelector(".faq-icon");
            icon.textContent = item.classList.contains("active") ? "−" : "+";
        });
    });
}

// Navbar Scroll Effect
function initNavbarScroll() {
    const navbar = document.getElementById("navbar");
    window.addEventListener("scroll", () => {
        if (window.scrollY > 40) {
            navbar.classList.add("scrolled");
        } else {
            navbar.classList.remove("scrolled");
        }
    });
}

// Toast Notification Helper
function showToast(message) {
    const container = document.getElementById("toastContainer");
    const toast = document.createElement("div");
    toast.className = "toast";
    toast.innerHTML = `<span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = "0";
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}
