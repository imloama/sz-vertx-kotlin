package sz.scaffold.tools

import jodd.util.Base64
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.Charset
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

//
// Created by kk on 17/9/12.
//
object RSAUtil {

    val rsaAlgorithm = "SHA1withRSA"

    /**
     * 创建一对公私秘钥, 并转化成 PEM 格式的文本字符串, 放在返回结果的Pair里
     * Pair.first: 公钥 Pair.second: 私钥
     */
    fun createPemKeyPair(): Pair<String, String> {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        val secRandom = SecureRandom()
        keyGen.initialize(1024, secRandom)

        val keyPair = keyGen.genKeyPair()

        val publicKeyPemObj = PemObject("PUBLIC KEY", keyPair.public.encoded)
        val pubWriter = StringWriter()
        PemWriter(pubWriter).use {
            it.writeObject(publicKeyPemObj)
        }

        val privateKeyPemObj = PemObject("PRIVATE KEY", keyPair.private.encoded)
        val privateWriter = StringWriter()

        PemWriter(privateWriter).use {
            it.writeObject(privateKeyPemObj)
        }

        return Pair(first = pubWriter.toString(), second = privateWriter.toString())
    }

    fun privateKeyFromPem(privateKeyPem: String): PrivateKey {
        val privReader = StringReader(privateKeyPem)

        return PemReader(privReader).use {
            val pemObj = it.readPemObject()
            val pkcs8Spec = PKCS8EncodedKeySpec(pemObj.content)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privKey = keyFactory.generatePrivate(pkcs8Spec)
            privKey
        }
    }

    fun publicKeyFromPem(publicKeyPem: String): PublicKey {
        val pubReader = StringReader(publicKeyPem)
        return PemReader(pubReader).use {
            val pemObj = it.readPemObject()
            val x509Spec = X509EncodedKeySpec(pemObj.content)
            val keyFactory = KeyFactory.getInstance("RSA")
            val pubKey = keyFactory.generatePublic(x509Spec)
            pubKey
        }
    }

    /**
     * 对指定的字符串进行加签
     * @return 返回加签后,经过base64编码后签名字符串
     */
    fun sign(data: String, privateKey: PrivateKey, charset: Charset = Charsets.UTF_8): String {
        val signature = Signature.getInstance(rsaAlgorithm)
        signature.initSign(privateKey)      // 私钥用来加签名

        signature.update(data.toByteArray(charset))

        return Base64.encodeToString(signature.sign())
    }

    /**
     * 调用此方法前, 请将mapData里不需要进行加签的字段过滤掉.此处, mapData 已经试经过过滤后的map
     * 对map里的键值对, 根据Key, 按照字母排序(升序), key=value 然后用 "&" 链接起来后的字符串进行加签
     * @return 返回加签后,经过base64编码后签名字符串
     */
    fun sign(mapData: Map<String, String>, privateKey: PrivateKey, charset: Charset = Charsets.UTF_8): String {
        val beSigned = mapData.toSortedMap().map { "${it.key}=${it.value}" }.joinToString("&")
        return sign(beSigned, privateKey, charset)
    }

    /**
     * 对指定的字符串和签名(经过base64编码后的签名字符串) 进行验证签名
     * @return 验签通过返回true
     */
    fun verify(data: String, signBase64: String, pubKey: PublicKey, charset: Charset = Charsets.UTF_8): Boolean {
        val sign = Base64.decode(signBase64)
        val signature = Signature.getInstance(rsaAlgorithm)
        signature.initVerify(pubKey)       // 公钥用来验证签名

        signature.update(data.toByteArray(charset))

        return signature.verify(sign)
    }

    /**
     * 调用此方法前, 请将mapData里不需要进行加签的字段过滤掉.此处, mapData 已经试经过过滤后的map
     * 对map里的键值对, 根据Key, 按照字母排序(升序), key=value 然后用 "&" 链接起来后的字符串进行验签
     * @return 验签通过返回true
     */
    fun verify(mapData: Map<String, String>, signBase64: String, pubKey: PublicKey, charset: Charset = Charsets.UTF_8): Boolean {
        val beSigned = mapData.toSortedMap().map { "${it.key}=${it.value}" }.joinToString("&")
        return verify(beSigned, signBase64, pubKey, charset)
    }
}