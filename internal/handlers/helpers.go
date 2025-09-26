package handlers

import (
	"event_social_platform/internal/models"
	"github.com/gin-gonic/gin"
)

// GetUserFromContext получает пользователя из контекста
func GetUserFromContext(c *gin.Context) *models.User {
	userInterface, exists := c.Get("user")
	if !exists {
		return nil
	}

	user, ok := userInterface.(*models.User)
	if !ok {
		return nil
	}

	return user
}
