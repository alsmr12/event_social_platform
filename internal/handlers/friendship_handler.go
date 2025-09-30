package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

type FriendshipHandler struct {
	friendshipRepo *repository.FriendshipRepository
	userRepo       *repository.UserRepository
}

func NewFriendshipHandler(friendshipRepo *repository.FriendshipRepository, userRepo *repository.UserRepository) *FriendshipHandler {
	return &FriendshipHandler{
		friendshipRepo: friendshipRepo,
		userRepo:       userRepo,
	}
}

// Отправить запрос в друзья
func (h *FriendshipHandler) SendFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Неверный ID пользователя",
		})
		return
	}

	// Нельзя добавить в друзья самого себя
	if currentUser.ID == uint(friendID) {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Нельзя добавить самого себя в друзья",
		})
		return
	}

	// Проверяем существование пользователя
	_, err = h.userRepo.GetUserByID(uint(friendID))
	if err != nil {
		c.HTML(http.StatusNotFound, "error.html", gin.H{
			"Error": "Пользователь не найден",
		})
		return
	}

	// Проверяем, не отправили ли уже запрос
	status, err := h.friendshipRepo.GetFriendshipStatus(currentUser.ID, uint(friendID))
	if err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка проверки статуса дружбы",
		})
		return
	}

	if status != "none" {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Запрос на дружбу уже отправлен или пользователь уже в друзьях",
		})
		return
	}

	friendship := &models.Friendship{
		UserID:   currentUser.ID,
		FriendID: uint(friendID),
		Status:   string(models.FriendshipPending),
	}

	if err := h.friendshipRepo.CreateFriendship(friendship); err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка отправки запроса в друзья",
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/profile/" + friendIDStr
	}
	c.Redirect(http.StatusSeeOther, referer)
}

// Принять запрос в друзья
func (h *FriendshipHandler) AcceptFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Неверный ID пользователя",
		})
		return
	}

	if err := h.friendshipRepo.AcceptFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка принятия запроса в друзья",
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/friends"
	}
	c.Redirect(http.StatusSeeOther, referer)
}

// Отклонить запрос в друзья
func (h *FriendshipHandler) RejectFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Неверный ID пользователя",
		})
		return
	}

	if err := h.friendshipRepo.RejectFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка отклонения запроса в друзья",
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/friends"
	}
	c.Redirect(http.StatusSeeOther, referer)
}

// Удалить из друзей
func (h *FriendshipHandler) RemoveFriend(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "error.html", gin.H{
			"Error": "Неверный ID пользователя",
		})
		return
	}

	if err := h.friendshipRepo.DeleteFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка удаления из друзей",
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/friends"
	}
	c.Redirect(http.StatusSeeOther, referer)
}

// Страница управления друзьями
func (h *FriendshipHandler) FriendsPage(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	// Получаем все данные для страницы
	friends, err := h.friendshipRepo.GetFriends(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка получения списка друзей",
		})
		return
	}

	pendingRequests, err := h.friendshipRepo.GetPendingRequests(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка получения входящих запросов",
		})
		return
	}

	sentRequests, err := h.friendshipRepo.GetSentRequests(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "error.html", gin.H{
			"Error": "Ошибка получения исходящих запросов",
		})
		return
	}

	friendsCount, _ := h.friendshipRepo.GetFriendsCount(currentUser.ID)

	c.HTML(http.StatusOK, "friends.html", gin.H{
		"User":            currentUser,
		"Friends":         friends,
		"PendingRequests": pendingRequests,
		"SentRequests":    sentRequests,
		"FriendsCount":    friendsCount,
	})
}
