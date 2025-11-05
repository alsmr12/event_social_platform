package models

// RegisterRequest — запрос на регистрацию (для JSON API)
type RegisterRequest struct {
    FirstName string `json:"first_name" binding:"required"`
    LastName  string `json:"last_name" binding:"required"`
    Email     string `json:"email" binding:"required,email"`
    Password  string `json:"password" binding:"required,min=6"`
    Gender    string `json:"gender" binding:"required,oneof=Мужской Женский"`
    Age       int    `json:"age" binding:"required"`
    Phone     string `json:"phone" binding:"required"`
}
