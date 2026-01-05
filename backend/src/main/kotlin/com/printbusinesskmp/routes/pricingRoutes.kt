package com.printbusinesskmp.routes

import com.printbusinesskmp.models.MaterialCosts
import com.printbusinesskmp.models.PricingRequest
import com.printbusinesskmp.models.TaxInfo
import com.printbusinesskmp.utils.PricingCalculator
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.configurePricingRoutes() {
    route("/api/pricing") {

        // POST /api/pricing/calculate - Calculate pricing for an order
        post("/calculate") {
            try {
                val request = call.receive<PricingRequest>()

                val pricingResponse = PricingCalculator.calculateFullPricing(
                    productType = request.productType,
                    quantity = request.quantity,
                    printArea = request.printArea,
                    laborMinutes = request.laborMinutes,
                    profitMarginPercent = request.profitMarginPercent,
                    materialCosts = request.materialCosts,
                    laborRatePerHour = request.laborRatePerHour
                )

                call.respond(HttpStatusCode.OK, pricingResponse)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"), "type" to e.javaClass.simpleName)
                )
            }
        }

        // GET /api/pricing/materials - Get current material costs
        get("/materials") {
            try {
                val materialCosts = MaterialCosts()
                call.respond(HttpStatusCode.OK, materialCosts)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // GET /api/pricing/tax-info - Get tax information
        get("/tax-info") {
            try {
                val taxInfo = TaxInfo()
                call.respond(HttpStatusCode.OK, taxInfo)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }
    }
}