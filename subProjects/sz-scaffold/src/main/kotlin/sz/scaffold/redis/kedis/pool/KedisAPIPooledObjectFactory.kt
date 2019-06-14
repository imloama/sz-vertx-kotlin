package sz.scaffold.redis.kedis.pool

import io.vertx.core.Vertx
import io.vertx.kotlin.redis.client.connectAwait
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions
import kotlinx.coroutines.runBlocking
import org.apache.commons.pool2.BasePooledObjectFactory
import org.apache.commons.pool2.PooledObject
import sz.scaffold.tools.logger.Logger

//
// Created by kk on 2019-06-11.
//
class KedisAPIPooledObjectFactory(private val vertx: Vertx,
                                  private val redisOptions: RedisOptions,
                                  private val operationTimeout:Long = -1) : BasePooledObjectFactory<KedisAPI>() {

    override fun wrap(obj: KedisAPI?): PooledObject<KedisAPI> {
        return KedisAPIPooledObject(obj)
    }

    override fun create(): KedisAPI = runBlocking {
        val client = Redis.createClient(vertx, redisOptions).connectAwait()
        val redisApi = KedisAPI(RedisAPI.api(client), operationTimeout)
        client.exceptionHandler {
            redisApi.markBroken()
            client.close()
//            Logger.debug("[exceptionHandler - markBroken and close] - Redis client occur exception: $it")
        }.endHandler {
            redisApi.markBroken()
//            Logger.debug("[endHandler - markBroken and close]")
        }
//        Logger.debug("[KedisAPIPooledObjectFactory] create a redis client")
        redisApi
    }

    override fun validateObject(p: PooledObject<KedisAPI>): Boolean {
        return p.`object`.broken.not()
    }
}