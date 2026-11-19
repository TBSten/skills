package io.github.shop.pricing

data class Cart(val items: List<Long>, val couponPercent: Int?)

object DiscountCalculator {
    fun total(cart: Cart): Long {
        val sum = cart.items.sum()
        val coupon = cart.couponPercent ?: return sum
        return sum - sum * coupon / 100
    }
}
