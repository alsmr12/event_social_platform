package models

import "time"

type User struct {
	ID          uint      `json:"id"`
	Email       string    `json:"email"`
	Password    string    `json:"-"` // потом с ним нужно что-то придумать
	FirstName   string    `json:"first_name"`
	LastName    string    `json:"last_name"`
	Gender      string    `json:"gender"`
	Age         int       `json:"age"`
	Phone       string    `json:"phone"`
	SocialLinks string    `json:"social_links"`
	CreatedAt   time.Time `json:"created_at"`
}

type CreateUserRequest struct {
	Email       string `form:"email" binding:"required"`
	Password    string `form:"password" binding:"required"`
	FirstName   string `form:"first_name" binding:"required"`
	LastName    string `form:"last_name" binding:"required"`
	Gender      string `form:"gender"`
	Age         int    `form:"age"`
	Phone       string `form:"phone"`
	SocialLinks string `form:"social_links"`
}
