package cn.vonfly;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * AES加密算法
 * 参数：
 * 1、秘钥长度(Key Size)
 * 三种key的长度128、192、256bits(1byte=8bits)
 * 即对应AES-128、AES-192、AES-256
 * <p>
 * 2、加密模式(Cipher Mode)
 * AES属于块加密(Block cipher)
 * 其中块加密工作模式
 * CBC、ECB、CTR、OFB、CFB等
 * 默认使用ECB
 * 3、填充方式(Padding)
 * 块加密只能对特定长度的数据块进行加密，因此CBC、ECB模式需要在 最后一个数据块加密前进行数据填充
 * CFB、OFB和CTR模式由于与Key进行加密操作的是上一块加密后的密文，因此不需要对最后一段明文进行填充
 * <p>
 * 4、初始向量(Initialization Vector)
 * 除了ECB以往的其他加密模式外需要传入一个初始向量。大小与BlockSize相等
 * AES的block Size为128bits
 * 加密解密时需要使用相同的初始向量
 * 加密解密都不传初始化向量是，默认使用一个全0的初始向量
 */
public class AESEncrypt {
    public static final String CIPHER_NAME = "AES";
    public static final String CIPHER_SPECIFIC_NAME = "AES/CFB/NoPadding";
    //指定初始向量值
    private static final byte[] initVector = "0000000000000000".getBytes();

    /**
     * 加密
     *
     * @param key
     * @param content
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public byte[] encrypt(byte[] key, byte[] content) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
        //1、构建key
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, CIPHER_NAME);

        //2、指定初始向量
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initVector);
        //
        Cipher cipher = Cipher.getInstance(CIPHER_SPECIFIC_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        return cipher.doFinal(content);
    }

    /**
     * 解密
     *
     * @param key
     * @param content
     * @return
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public byte[] decrypt(byte[] key, byte[] content) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        //1、构建key
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, CIPHER_NAME);

        //2、指定初始向量
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initVector);
        //
        Cipher cipher = Cipher.getInstance(CIPHER_SPECIFIC_NAME);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        return cipher.doFinal(content);
    }

    public static void main(String[] args) {
        String key = "6P(g*(%gYDrBggFk";
        byte[] bytes = key.getBytes(Charset.forName("UTF-8"));
        AESEncrypt aesEncrypt = new AESEncrypt();
        try {
            byte[] encrypt = aesEncrypt.encrypt(bytes, bytes);
            byte[] decrypt = aesEncrypt.decrypt(bytes, encrypt);
            System.out.println(encrypt);
            System.out.println(new String(decrypt, Charset.forName("UTF-8")));

        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }
}
