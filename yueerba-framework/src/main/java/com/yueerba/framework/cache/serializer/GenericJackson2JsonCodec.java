package com.yueerba.framework.cache.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Description: Redisson的自定义编解码器，用于将对象序列化为JSON并进行Redis存储与读取
 * Author: yueerba
 * Date: 2023/9/12
 */
@Component
public class GenericJackson2JsonCodec extends BaseCodec {

    @Resource
    private RedisSerializer jackson2JsonRedisSerializer;

    /**
     * 获取值解码器，将Redis中的数据反序列化为Java对象
     *
     * @return 值解码器，用于将Redis中的数据反序列化为Java对象
     */
    @Override
    public Decoder<Object> getValueDecoder() {
        return (buf, state) -> {
            // 从ByteBuf中读取字节数组
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);

            // 使用jackson2JsonRedisSerializer反序列化字节数组为Java对象
            return jackson2JsonRedisSerializer.deserialize(bytes);
        };
    }

    /**
     * 获取值编码器，将Java对象序列化为Redis存储的数据
     *
     * @return 值编码器，用于将Java对象序列化为Redis存储的数据
     */
    @Override
    public Encoder getValueEncoder() {
        return in -> {
            ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufOutputStream os = new ByteBufOutputStream(out);

                // 使用jackson2JsonRedisSerializer将Java对象序列化为字节数组，并写入输出流
                os.write(jackson2JsonRedisSerializer.serialize(in));

                // 返回包含序列化数据的ByteBuf
                return os.buffer();
            } catch (IOException e) {
                out.release();
                throw e;
            } catch (Exception e) {
                out.release();
                throw new IOException(e);
            }
        };
    }
}
