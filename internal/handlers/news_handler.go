package handlers

import (
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"
	"time"
	"github.com/gin-gonic/gin"
)

type NewsHandler struct {
	newsRepo *repository.NewsRepository
}

func NewNewsHandler(newsRepo *repository.NewsRepository) *NewsHandler {
	return &NewsHandler{
		newsRepo: newsRepo,
	}
}

// Показать ленту новостей
func (h *NewsHandler) ShowNewsFeed(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	// Получаем параметры пагинации
	page := getPageParam(c)
	postsPerPage := 10
	eventsPerPage := 8

	// Рассчитываем offset
	postsOffset := (page - 1) * postsPerPage
	eventsOffset := (page - 1) * eventsPerPage

	// Получаем посты для ленты
	posts, err := h.newsRepo.GetNewsFeed(currentUser.ID, postsPerPage, postsOffset)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Новости",
			"NavActive":   "news",
			"Error":       "Ошибка загрузки ленты новостей",
			"CurrentUser": currentUser,
		})
		return
	}

	// Получаем события для ленты
	events, err := h.newsRepo.GetEventsFeed(currentUser.ID, eventsPerPage, eventsOffset)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Новости",
			"NavActive":   "news",
			"Error":       "Ошибка загрузки событий",
			"CurrentUser": currentUser,
		})
		return
	}

	// Получаем общее количество для пагинации
	totalPosts, err := h.newsRepo.GetTotalPostsCount(currentUser.ID)
	if err != nil {
		totalPosts = 0
	}

	totalEvents, err := h.newsRepo.GetTotalEventsCount(currentUser.ID)
	if err != nil {
		totalEvents = 0
	}

	// Определяем общее количество страниц (по максимальному из двух)
	totalItems := totalPosts
	if totalEvents > totalPosts {
		totalItems = totalEvents
	}
	totalPages := int((totalItems + int64(postsPerPage) - 1) / int64(postsPerPage))
	if totalPages == 0 {
		totalPages = 1
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":        "Новости",
		"NavActive":    "news",
		"Posts":        posts,
		"Events":       events,
		"CurrentUser":  currentUser,
		"CurrentPage":  page,
		"TotalPages":   totalPages,
		"PostsPerPage": postsPerPage,
		"EventsPerPage": eventsPerPage,
		"TotalPosts":   totalPosts,
		"TotalEvents":  totalEvents,
	})
}

// Вспомогательная функция для получения номера страницы
func getPageParam(c *gin.Context) int {
	pageStr := c.Query("page")
	page, err := strconv.Atoi(pageStr)
	if err != nil || page < 1 {
		return 1
	}
	return page
}

// В news_handler.go добавьте:

// GetNewsFeedJSON - получить ленту новостей (JSON)
func (h *NewsHandler) GetNewsFeedJSON(c *gin.Context) {
    currentUser := GetUserFromContext(c)
    if currentUser == nil {
        c.JSON(http.StatusUnauthorized, gin.H{
            "success": false,
            "message": "Не авторизован",
        })
        return
    }

    // 1. Получаем список ID пользователей, на которых подписан текущий пользователь
    subscriptionRepo := repository.NewSubscriptionRepository(h.newsRepo.GetDB())
    following, err := subscriptionRepo.GetFollowing(currentUser.ID)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "success": false,
            "message": "Ошибка получения подписок",
        })
        return
    }

    // Если нет подписок - возвращаем пустую ленту
    if len(following) == 0 {
        c.JSON(http.StatusOK, gin.H{
            "success": true,
            "posts":   []gin.H{},
            "events":  []gin.H{},
            "message": "Подпишитесь на пользователей, чтобы видеть их записи",
        })
        return
    }

    // 2. Собираем ID пользователей для фильтрации
    var followingIDs []uint
    for _, user := range following {
        followingIDs = append(followingIDs, user.ID)
    }

    // 3. Получаем посты ТОЛЬКО от пользователей, на которых подписан
    posts, err := h.newsRepo.GetPostsFromUsers(followingIDs, 50, 0)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "success": false,
            "message": "Ошибка загрузки ленты новостей",
        })
        return
    }

    // 4. Получаем события ТОЛЬКО от пользователей, на которых подписан
    events, err := h.newsRepo.GetEventsFromUsers(followingIDs, 50, 0)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "success": false,
            "message": "Ошибка загрузки событий",
        })
        return
    }

    // 5. Преобразуем посты в нужный формат
    var postItems []gin.H
    for _, post := range posts {
        postItems = append(postItems, gin.H{
            "id":         post.ID,
            "type":       "post",
            "content":    post.Content,
            "author": gin.H{
                "id":         post.Author.ID,
                "email":      post.Author.Email,
                "first_name": post.Author.FirstName,
                "last_name":  post.Author.LastName,
                "gender":     post.Author.Gender,
                "age":        post.Author.Age,
                "phone":      post.Author.Phone,
            },
            "created_at": post.CreatedAt.Format(time.RFC3339),
            "post": gin.H{
                "id":        post.ID,
                "content":   post.Content,
                "author_id": post.AuthorID,
                "user_id":   post.UserID,
                "created_at": post.CreatedAt.Format(time.RFC3339),
                "updated_at": post.UpdatedAt.Format(time.RFC3339),
            },
        })
    }

    // 6. Преобразуем события в нужный формат
    var eventItems []gin.H
    for _, event := range events {
        eventItems = append(eventItems, gin.H{
            "id":         event.ID,
            "type":       "event",
            "content":    event.Title + " - " + event.Description,
            "author": gin.H{
                "id":         event.Creator.ID,
                "email":      event.Creator.Email,
                "first_name": event.Creator.FirstName,
                "last_name":  event.Creator.LastName,
                "gender":     event.Creator.Gender,
                "age":        event.Creator.Age,
                "phone":      event.Creator.Phone,
            },
            "created_at": event.CreatedAt.Format(time.RFC3339),
            "event": gin.H{
                "id":          event.ID,
                "title":       event.Title,
                "description": event.Description,
                "type":        event.Type,
                "date_time":   event.DateTime.Format(time.RFC3339),
                "location":    event.Location,
                "creator_id":  event.CreatorID,
                "is_private":  event.IsPrivate,
                "created_at":  event.CreatedAt.Format(time.RFC3339),
            },
        })
    }

    c.JSON(http.StatusOK, gin.H{
        "success": true,
        "posts":   postItems,
        "events":  eventItems,
        "message": "Лента новостей загружена",
    })
}