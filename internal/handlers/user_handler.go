package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

type UserHandler struct {
	userRepo *repository.UserRepository
}

func NewUserHandler(userRepo *repository.UserRepository) *UserHandler {
	return &UserHandler{userRepo: userRepo}
}

func (h *UserHandler) ShowHomePage(c *gin.Context) {
	c.HTML(http.StatusOK, "index.html", gin.H{
		"Title": "Социальная платформа",
	})
}

func (h *UserHandler) ShowCreateProfileForm(c *gin.Context) {
	c.HTML(http.StatusOK, "create_profile.html", gin.H{})
}

func (h *UserHandler) CreateProfile(c *gin.Context) {
	var req models.CreateUserRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "create_profile.html", gin.H{
			"Error": "Неверные данные формы",
		})
		return
	}

	if h.userRepo.UserExists(req.Email) {
		c.HTML(http.StatusBadRequest, "create_profile.html", gin.H{
			"Error": "Пользователь с таким email уже существует",
		})
		return
	}

	// Создаем пользователя (БЕЗ пароля пока)
	user := &models.User{
		Email:       req.Email,
		FirstName:   req.FirstName,
		LastName:    req.LastName,
		Gender:      req.Gender,
		Age:         req.Age,
		Phone:       req.Phone,
		SocialLinks: req.SocialLinks,
	}

	// Хешируем пароль!
	if err := user.HashPassword(req.Password); err != nil {
		c.HTML(http.StatusInternalServerError, "create_profile.html", gin.H{
			"Error": "Ошибка при хешировании пароля: " + err.Error(),
		})
		return
	}

	// Сохраняем в БД
	if err := h.userRepo.CreateUser(user); err != nil {
		c.HTML(http.StatusInternalServerError, "create_profile.html", gin.H{
			"Error": "Ошибка при создании профиля: " + err.Error(),
		})
		return
	}

	c.Redirect(http.StatusSeeOther, "/login")
}

func (h *UserHandler) GetProfile(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusOK, "profile.html", gin.H{
			"Error": "Неверный ID профиля",
		})
		return
	}

	user, err := h.userRepo.GetUserByID(uint(id))
	if err != nil || user == nil {
		c.HTML(http.StatusOK, "profile.html", gin.H{
			"Error": "Профиль не найден",
		})
		return
	}

	c.HTML(http.StatusOK, "profile.html", gin.H{
		"User": user,
	})
}

func (h *UserHandler) GetAllProfiles(c *gin.Context) {
	users, err := h.userRepo.GetAllUsers()
	if err != nil {
		// Всегда возвращаем Users, даже если пустой массив
		c.HTML(http.StatusOK, "profiles.html", gin.H{
			"Users": []*models.User{},
		})
		return
	}

	// Если users nil, заменяем на пустой массив
	if users == nil {
		users = []*models.User{}
	}

	c.HTML(http.StatusOK, "profiles.html", gin.H{
		"Users": users,
	})
}
