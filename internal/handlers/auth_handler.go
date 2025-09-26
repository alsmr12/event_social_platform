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
	c.HTML(http.StatusOK, "login.html", gin.H{
		"Title": "Вход в систему",
	})
}

func (h *AuthHandler) Login(c *gin.Context) {
	println("\n=== ПОПЫТКА ВХОДА ===")

	var req models.LoginRequest
	if err := c.ShouldBind(&req); err != nil {
		println("❌ Ошибка绑定 формы:", err.Error())
		c.HTML(http.StatusBadRequest, "login.html", gin.H{
			"Error": "Неверные данные формы",
		})
		return
	}

	println("Email из формы:", req.Email)

	// Ищем пользователя по email
	user, err := h.userRepo.GetUserByEmail(req.Email)
	if err != nil {
		println("ПОЛЬЗОВАТЕЛЬ НЕ НАЙДЕН")
		c.HTML(http.StatusBadRequest, "login.html", gin.H{
			"Error": "Неверный email или пароль",
		})
		return
	}

	println("Пользователь найден - ID:", user.ID, "Email:", user.Email)

	// Проверяем пароль
	if !user.CheckPassword(req.Password) {
		println("НЕВЕРНЫЙ ПАРОЛЬ")
		c.HTML(http.StatusBadRequest, "login.html", gin.H{
			"Error": "Неверный email или пароль",
		})
		return
	}

	println("ПАРОЛЬ ВЕРНЫЙ")

	// Создаем сессию
	token := uuid.New().String()
	session := &models.Session{
		UserID:    user.ID,
		Token:     token,
		ExpiresAt: time.Now().Add(24 * time.Hour),
	}

	println("Создаем сессию - UserID:", user.ID, "Token:", token)

	if err := h.sessionRepo.CreateSession(session); err != nil {
		println("ОШИБКА СОЗДАНИЯ СЕССИИ:", err.Error())
		c.HTML(http.StatusInternalServerError, "login.html", gin.H{
			"Error": "Ошибка создания сессии",
		})
		return
	}

	// Устанавливаем куку
	c.SetCookie("session_token", token, 3600*24, "/", "", false, true)
	println("КУКА УСТАНОВЛЕНА")

	println("ВХОД УСПЕШЕН - перенаправляем на /profile")
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
		println("❌ ПОЛЬЗОВАТЕЛЬ НЕ АУТЕНТИФИЦИРОВАН - редирект на логин")
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	println("ОТОБРАЖЕНИЕ ПРОФИЛЯ для:", user.ID, user.Email)
	c.HTML(http.StatusOK, "my_profile.html", gin.H{
		"User": user,
	})
}
