/**
 * Универсальная функция пагинации для загрузки постов
 * @param {string} containerSelector - CSS селектор контейнера для постов
 * @param {string} loadUrl - URL для загрузки данных
 * @param {Object} options - Дополнительные параметры
 */
function setupPagination(containerSelector, loadUrl, options = {}) {
    const container = document.querySelector(containerSelector);
    if (!container) return;

    const defaultOptions = {
        postsPerPage: 10,
        maxVisiblePages: 5,
        loadingText: 'Загрузка...',
        errorText: 'Ошибка загрузки данных'
    };

    const config = { ...defaultOptions, ...options };
    let currentPage = 1;
    let totalPages = 1;
    let isLoading = false;

    // Инициализация
    function init() {
        loadPosts(currentPage);
    }

    // Загрузка постов
    async function loadPosts(page) {
        if (isLoading) return;
        
        isLoading = true;
        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'loading';
        loadingDiv.textContent = config.loadingText;
        container.appendChild(loadingDiv);
        
        try {
            const response = await fetch(`${loadUrl}?page=${page}&limit=${config.postsPerPage}`);
            if (!response.ok) throw new Error('Network response was not ok');
            
            const data = await response.json();
            
            if (data.success) {
                // Очищаем контейнер, кроме элементов пагинации
                Array.from(container.children).forEach(child => {
                    if (!child.classList.contains('pagination') && !child.classList.contains('loading')) {
                        child.remove();
                    }
                });
                
                // Добавляем новые посты
                if (data.posts && data.posts.length > 0) {
                    data.posts.forEach(post => {
                        const postElement = createPostElement(post);
                        container.appendChild(postElement);
                    });
                    
                    // Обновляем общее количество страниц (предполагаем, что сервер возвращает это в ответе)
                    if (data.total_pages) {
                        totalPages = data.total_pages;
                    } else {
                        // Альтернативный расчет, если сервер не возвращает total_pages
                        totalPages = Math.ceil(data.posts.length / config.postsPerPage) || 1;
                    }
                } else {
                    const emptyMessage = document.createElement('div');
                    emptyMessage.className = 'empty-state';
                    emptyMessage.textContent = 'Записей пока нет';
                    container.appendChild(emptyMessage);
                }
                
                // Обновляем пагинацию
                updatePagination();
            } else {
                throw new Error(data.message || 'Unknown error');
            }
        } catch (error) {
            console.error('Error loading posts:', error);
            
            // Показываем сообщение об ошибке
            const errorDiv = document.createElement('div');
            errorDiv.className = 'error';
            errorDiv.textContent = config.errorText;
            container.appendChild(errorDiv);
        } finally {
            // Удаляем индикатор загрузки
            const loadingDiv = container.querySelector('.loading');
            if (loadingDiv) loadingDiv.remove();
            isLoading = false;
        }
    }

    // Создание элемента поста
    function createPostElement(post) {
        const postDiv = document.createElement('div');
        postDiv.className = 'post';
        
        const author = post.author || {};
        const createdAt = new Date(post.created_at);
        
        postDiv.innerHTML = `
            <div class="post-header">
                <span class="post-author">\