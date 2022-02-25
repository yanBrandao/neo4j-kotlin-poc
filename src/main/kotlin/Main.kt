import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Values.parameters
import java.util.UUID

fun createDriver(): Driver = GraphDatabase.driver(
    "bolt://localhost:7687",
    AuthTokens.basic( "neo4j", "test"))

fun createOnboarding(onboardingId: String) {
    val driver = createDriver()
    try {
        val session = driver.session()

        val greeting = session.writeTransaction {
            val result  = it.run(
                "CREATE (a:Onboarding) SET a.onboarding_id = '$onboardingId' RETURN a.onboarding_id + ', from node ' + id(a)",
                parameters("message", onboardingId)
            )
            return@writeTransaction result.single().get(0).asString()
        }

        println(greeting)
    } catch (ex: Exception) {
        println("Could not open session ${ex.message}")
    } finally {
        driver.close()
    }
}

fun makeOnboardingRelation(onboardingId: String, step: String): String {
    val driver = createDriver()
    try {
        val session = driver.session()
        val match = session.writeTransaction {
            val result  = it.run(
                """
                    MATCH (o:Onboarding {onboarding_id:'$onboardingId'})
                    CREATE (f:$step {name: '$step'})
                        CREATE (f)<-[:REQUEST]-(o)
                        RETURN id(f)
                    """,
                parameters("message", onboardingId)
            )
            return@writeTransaction result.single().get(0)
        }
        println("Onboarding-id ($onboardingId) connected to $match of label $step")
        return match.toString()
    } catch (ex: Exception) {
        println("Could not open session ${ex.message}")
    } finally {
        driver.close()
    }
    return ""
}

fun makeOtherRelation(label: String, id: String, step: String, relationMessage: String): String {
    val driver = createDriver()
    try {
        val session = driver.session()
        val match = session.writeTransaction {
            val result  = it.run(
                """
                    MATCH (o:$label {id:'$id'})
                    CREATE (f:$step {name: '$step'})
                    CREATE (f)<-[:$relationMessage]-(o)
                    RETURN id(f)
                    """,
                parameters("id", id)
            )
            return@writeTransaction result.single().get(0).asInt()
        }

        return match.toString()

    } catch (ex: Exception) {
        println("Could not open session ${ex.message}")
    } finally {
        driver.close()
    }
    return ""
}

fun main(args: Array<String>) {

    val onboardingId = UUID.randomUUID().toString()

    createOnboarding(onboardingId)

    val fraudId = makeOnboardingRelation(onboardingId, "Fraud")
    val biometricId = makeOnboardingRelation(onboardingId, "Biometric")
    val userValidationId = makeOtherRelation("Fraud", fraudId, "UserValidation", "IS_VALID_BY")
    makeOnboardingRelation(onboardingId, "Customer")
    makeOnboardingRelation(onboardingId, "Account")
    makeOnboardingRelation(onboardingId, "Notification")
    makeOnboardingRelation(onboardingId, "Credential")


}