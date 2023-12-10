extern "C" {
#include <include/libavformat/avformat.h>
#include <include/libavcodec/avcodec.h>
#include <include/libavcodec/codec.h>
#include <include/libavutil/avutil.h>
#include <include/libavutil/pixdesc.h>
}


AVPixelFormat hw_pix_fmt;
static enum AVPixelFormat get_hw_format(AVCodecContext *ctx,
                                        const enum AVPixelFormat *pix_fmts)
{
    const enum AVPixelFormat *p;

    for (p = pix_fmts; *p != -1; p++) {
        if (*p == hw_pix_fmt)
            return *p;
    }
    LOGD_E("FFDecoder","Failed to get HW surface format.\n");
    return AV_PIX_FMT_NONE;
}

void FFDecoder::decodeVideo(const char *videoPath, const char *yuvPath) {
    AVFormatContext *avFormatContext = avformat_alloc_context();
    int ret = avformat_open_input(&avFormatContext, videoPath, nullptr, nullptr);
    if (ret < 0) {
        LOGD_E("FFDecoder","打开媒体文件失败");
        return;
    }
    avformat_find_stream_info(avFormatContext, nullptr);
    int video_index = av_find_best_stream(avFormatContext, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
    if (video_index < 0) {
        LOGD_E("FFDecoder","找不到视频索引");
        return;
    }
    LOGD_E("FFDecoder","找到视频索引:%d", video_index);

    const AVCodec *avCodec = nullptr;
    AVCodecContext *avCodecContext = nullptr;
    AVPacket *avPacket = nullptr;
    AVFrame *avFrame = nullptr;
    FILE *yuv_file = nullptr;
    switch (avFormatContext->streams[video_index]->codecpar->codec_id) {
        // 这里以h264为例
        case AV_CODEC_ID_H264:
            avCodec = avcodec_find_decoder_by_name("h264_mediacodec");
            if (nullptr == avCodec) {
                LOGD_E("FFDecoder","没有找到硬解码器h264_mediacodec");
                return;
            } else {
                // 配置硬解码器
                int i;
                for (i = 0;; i++) {
                    const AVCodecHWConfig *config = avcodec_get_hw_config(avCodec, i);
                    if (nullptr == config) {
                        LOGD_E("FFDecoder","获取硬解码是配置失败");
                        return;
                    }
                    if (config->methods & AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX &&
                        config->device_type == AV_HWDEVICE_TYPE_MEDIACODEC) {
                        hw_pix_fmt = config->pix_fmt;
                        LOGD_E("FFDecoder","硬件解码器配置成功");
                        break;
                    }
                }
                break;
            }
    }
    avCodecContext = avcodec_alloc_context3(avCodec);
    avcodec_parameters_to_context(avCodecContext,avFormatContext->streams[video_index]->codecpar);
    avCodecContext->get_format = get_hw_format;
    // 硬件解码器初始化
    AVBufferRef *hw_device_ctx = nullptr;
    ret = av_hwdevice_ctx_create(&hw_device_ctx, AV_HWDEVICE_TYPE_MEDIACODEC,
                                 nullptr, nullptr, 0);
    if (ret < 0) {
        LOGD_E("FFDecoder","Failed to create specified HW device");
        return;
    }
    avCodecContext->hw_device_ctx = av_buffer_ref(hw_device_ctx);
    // 打开解码器
    ret = avcodec_open2(avCodecContext, avCodec, nullptr);
    if (ret != 0) {
        LOGD_E("FFDecoder","解码器打开失败:%s",av_err2str(ret));
        return;
    } else {
        LOGD_E("FFDecoder","解码器打开成功");
    }

    avPacket = av_packet_alloc();
    avFrame = av_frame_alloc();
    yuv_file = fopen(yuvPath,"wb");
    while (true) {
        ret = av_read_frame(avFormatContext, avPacket);
        if (ret != 0) {
            LOGD_D("FFDecoder","av_read_frame end");
            // todo可能解码器内还有缓存的数据，需要avcodec_send_packet空包进行冲刷
            break;
        }
        if(avPacket->stream_index != video_index){
            av_packet_unref(avPacket);
            continue;
        }
        ret = avcodec_send_packet(avCodecContext,avPacket);
        if(ret == AVERROR(EAGAIN)){
            LOGD_E("FFDecoder","avcodec_send_packet EAGAIN");
        } else if(ret < 0){
            LOGD_E("FFDecoder","avcodec_send_packet fail:%s",av_err2str(ret));
            return;
        }
        av_packet_unref(avPacket);
        ret = avcodec_receive_frame(avCodecContext,avFrame);
        LOGD_E("FFDecoder","avcodec_receive_frame：%d",ret);
        while (ret == 0){
            LOGD_D("FFDecoder","获取解码数据成功：%s",av_get_pix_fmt_name(static_cast<AVPixelFormat>(avFrame->format)));
            LOGD_D("FFDecoder","linesize0:%d,linesize1:%d,linesize2:%d",avFrame->linesize[0],avFrame->linesize[1],avFrame->linesize[2]);
            LOGD_D("FFDecoder","width:%d,height:%d",avFrame->width,avFrame->height);
            ret = avcodec_receive_frame(avCodecContext,avFrame);
            // 如果解码出来的数据是nv12
            // 播放 ffplay -i d:/cap.yuv -pixel_format nv12 -framerate 25 -video_size 640x480
            // 写入y
            for(int j=0; j<avFrame->height; j++)
                fwrite(avFrame->data[0] + j * avFrame->linesize[0], 1, avFrame->width, yuv_file);
            // 写入uv
            for(int j=0; j<avFrame->height/2; j++)
                fwrite(avFrame->data[1] + j * avFrame->linesize[1], 1, avFrame->width, yuv_file);
        }
    }
    // 资源释放
    if (nullptr != avFormatContext) {
        avformat_free_context(avFormatContext);
        avFormatContext = nullptr;
    }
    if (nullptr != avCodecContext) {
        avcodec_free_context(&avCodecContext);
        avCodecContext = nullptr;
    }
    if (nullptr != avPacket) {
        av_packet_free(&avPacket);
        avPacket = nullptr;
    }
    if (nullptr != avFrame) {
        av_frame_free(&avFrame);
        avFrame = nullptr;
    }
    if(nullptr != yuv_file){
        fclose(yuv_file);
        yuv_file = nullptr;
    }
}