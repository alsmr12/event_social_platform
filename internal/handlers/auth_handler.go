package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type AuthHandler struct {
	userRepo    *repository.UserRepository
	sessionRepo *repository.SessionRepository
}

func NewAuthHandler(userRepo *repository.UserRepository, sessionRepo *repository.SessionRepository) *AuthHandler {
	return &AuthHandler{
		userRepo:    userRepo,
		sessionRepo: sessionRepo,
	}
}

func (h *AuthHandler) ShowLoginForm(c *gin.Context) {
	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":     "Вход в систему",
		"NavActive": "login",
	})
}

func (h *AuthHandler) Login(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный email или пароль",
		})
		return
	}

	// Ищем пользователя по email
	user, err := h.userRepo.GetUserByEmail(req.Email)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный email",
		})
		return
	}

	// Проверяем пароль
	if !user.CheckPassword(req.Password) {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Неверный пароль",
		})
		return
	}

	// Создаем сессию
	token := uuid.New().String()
	session := &models.Session{
		UserID:    user.ID,
		Token:     token,
		ExpiresAt: time.Now().Add(24 * time.Hour),
	}

	if err := h.sessionRepo.CreateSession(session); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":     "Вход в систему",
			"NavActive": "login",
			"Error":     "Ошибка создания сессии",
		})
		return
	}

	// Устанавливаем куку
	c.SetCookie("session_token", token, 3600*24, "/", "", false, true)
	c.Redirect(http.StatusSeeOther, "/profile")
}

func (h *AuthHandler) Logout(c *gin.Context) {
	token, err := c.Cookie("session_token")
	if err == nil {
		h.sessionRepo.DeleteSession(token)
	}

	c.SetCookie("session_token", "", -1, "/", "", false, true)
	c.Redirect(http.StatusSeeOther, "/")
}

func (h *AuthHandler) ShowProfile(c *gin.Context) {
	user := GetUserFromContext(c)
	if user == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	// Получаем записи на стене пользователя
	db := h.userRepo.GetDB()
	wallRepo := repository.NewWallRepository(db)
	posts, err := wallRepo.GetPostsByUserID(user.ID)
	if err != nil {
		posts = []*models.WallPost{}
	}

	// Получаем статистику подписок
	subscriptionRepo := repository.NewSubscriptionRepository(db)
	followersCount, _ := subscriptionRepo.GetFollowersCount(user.ID)
	followingCount, _ := subscriptionRepo.GetFollowingCount(user.ID)

	// Получаем статистику друзей
	friendshipRepo := repository.NewFriendshipRepository(db)
	friendsCount, _ := friendshipRepo.GetFriendsCount(user.ID)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":          "Мой профиль",
		"NavActive":      "my_profile",
		"User":           user,
		"Posts":          posts,
		"FollowersCount": followersCount,
		"FollowingCount": followingCount,
		"FriendsCount":   friendsCount,
		"CurrentUser":    user,
	})
}
