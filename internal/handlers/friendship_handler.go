package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

type FriendshipHandler struct {
	friendshipRepo  *repository.FriendshipRepository
	userRepo        *repository.UserRepository
	achievementRepo *repository.AchievementRepository
}

func NewFriendshipHandler(friendshipRepo *repository.FriendshipRepository, userRepo *repository.UserRepository, achievementRepo *repository.AchievementRepository) *FriendshipHandler {
	return &FriendshipHandler{
		friendshipRepo:  friendshipRepo,
		userRepo:        userRepo,
		achievementRepo: achievementRepo,
	}
}

func (h *FriendshipHandler) SendFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	// Нельзя добавить в друзья самого себя
	if currentUser.ID == uint(friendID) {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Нельзя добавить самого себя в друзья",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем существование пользователя
	_, err = h.userRepo.GetUserByID(uint(friendID))
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Пользователь не найден",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем, не отправили ли уже запрос
	status, err := h.friendshipRepo.GetFriendshipStatus(currentUser.ID, uint(friendID))
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка проверки статуса дружбы",
			"CurrentUser": currentUser,
		})
		return
	}

	if status != "none" {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Запрос на дружбу уже отправлен или пользователь уже в друзьях",
			"CurrentUser": currentUser,
		})
		return
	}

	friendship := &models.Friendship{
		UserID:   currentUser.ID,
		FriendID: uint(friendID),
		Status:   string(models.FriendshipPending),
	}

	if err := h.friendshipRepo.CreateFriendship(friendship); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка отправки запроса в друзья",
			"CurrentUser": currentUser,
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

func (h *FriendshipHandler) AcceptFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	if err := h.friendshipRepo.AcceptFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка принятия запроса в друзья",
			"CurrentUser": currentUser,
		})
		return
	}
	//  Обновляем достижения при добавлении друга (для обоих пользователей)
	if h.achievementRepo != nil {
		go h.achievementRepo.UpdateAchievementsOnFriendshipAdded(currentUser.ID)
		go h.achievementRepo.UpdateAchievementsOnFriendshipAdded(uint(friendID))
	}
	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/friends"
	}
	c.Redirect(http.StatusSeeOther, referer)
}

func (h *FriendshipHandler) RejectFriendRequest(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	if err := h.friendshipRepo.RejectFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка отклонения запроса в друзья",
			"CurrentUser": currentUser,
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

func (h *FriendshipHandler) RemoveFriend(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	friendIDStr := c.Param("id")
	friendID, err := strconv.Atoi(friendIDStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID пользователя",
			"CurrentUser": currentUser,
		})
		return
	}

	if err := h.friendshipRepo.DeleteFriendship(currentUser.ID, uint(friendID)); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка удаления из друзей",
			"CurrentUser": currentUser,
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

func (h *FriendshipHandler) FriendsPage(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	// Получаем все данные для страницы
	friends, err := h.friendshipRepo.GetFriends(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка получения списка друзей",
			"CurrentUser": currentUser,
		})
		return
	}

	pendingRequests, err := h.friendshipRepo.GetPendingRequests(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка получения входящих запросов",
			"CurrentUser": currentUser,
		})
		return
	}

	sentRequests, err := h.friendshipRepo.GetSentRequests(currentUser.ID)
	if err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка получения исходящих запросов",
			"CurrentUser": currentUser,
		})
		return
	}

	friendsCount, _ := h.friendshipRepo.GetFriendsCount(currentUser.ID)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":           "Мои друзья",
		"NavActive":       "friends", // УЖЕ ПРАВИЛЬНО
		"User":            currentUser,
		"Friends":         friends,
		"PendingRequests": pendingRequests,
		"SentRequests":    sentRequests,
		"FriendsCount":    friendsCount,
		"CurrentUser":     currentUser,
	})
}
