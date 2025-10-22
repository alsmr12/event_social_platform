package handlers

import (
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

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