#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"

#include <jni.h>
#include <atomic>
#include <chrono>
#include <cstdio>
#include <functional>
#include <future>
#include <memory>
#include <mutex>
#include <queue>
#include <string>
#include <thread>

namespace {

std::shared_ptr<discordpp::Client> gClient;
std::thread gPumpThread;
std::atomic<bool> gRunning{false};

std::mutex gTaskMutex;
std::queue<std::function<void()>> gTasks;

void post(std::function<void()> task) {
    std::lock_guard<std::mutex> lock(gTaskMutex);
    gTasks.push(std::move(task));
}

void drainTasks() {
    std::queue<std::function<void()>> local;
    {
        std::lock_guard<std::mutex> lock(gTaskMutex);
        std::swap(local, gTasks);
    }
    while (!local.empty()) {
        local.front()();
        local.pop();
    }
}

std::string jsonEscape(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 8);
    for (char c : s) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    char buf[8];
                    std::snprintf(buf, sizeof(buf), "\\u%04x", static_cast<unsigned char>(c));
                    out += buf;
                } else {
                    out += c;
                }
        }
    }
    return out;
}

std::string jstr(JNIEnv* env, jstring s) {
    if (!s) return "";
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string out(c ? c : "");
    env->ReleaseStringUTFChars(s, c);
    return out;
}

std::string awaitJson(std::function<std::string()> read, const char* fallback) {
    auto promise = std::make_shared<std::promise<std::string>>();
    auto future = promise->get_future();
    post([promise, read]() { promise->set_value(read()); });
    if (future.wait_for(std::chrono::seconds(5)) != std::future_status::ready) return fallback;
    return future.get();
}

void pumpLoop() {
    while (gRunning.load()) {
        drainTasks();
        discordpp::RunCallbacks();
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
}

}  // namespace

extern "C" {

JNIEXPORT void JNICALL
Java_com_psplauncher_discord_DiscordNativeBridge_nativeInit(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong applicationId) {
    if (gClient) return;
    gClient = std::make_shared<discordpp::Client>();
    gClient->SetApplicationId(static_cast<uint64_t>(applicationId));
    gRunning.store(true);
    gPumpThread = std::thread(pumpLoop);
}

JNIEXPORT jboolean JNICALL
Java_com_psplauncher_discord_DiscordNativeBridge_nativeUpdateToken(
    JNIEnv* env, jobject /*thiz*/, jstring jToken) {
    if (!gClient) return JNI_FALSE;
    const std::string token = jstr(env, jToken);

    auto promise = std::make_shared<std::promise<bool>>();
    auto future = promise->get_future();
    post([token, promise]() {
        gClient->UpdateToken(
            discordpp::AuthorizationTokenType::Bearer, token,
            [promise](discordpp::ClientResult result) {
                if (result.Successful()) {
                    gClient->Connect();
                    promise->set_value(true);
                } else {
                    promise->set_value(false);
                }
            });
    });

    if (future.wait_for(std::chrono::seconds(30)) != std::future_status::ready) return JNI_FALSE;
    return future.get() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_psplauncher_discord_DiscordNativeBridge_nativeGetCurrentUserJson(
    JNIEnv* env, jobject /*thiz*/) {
    if (!gClient) return env->NewStringUTF("");
    const std::string result = awaitJson([]() -> std::string {
        auto user = gClient->GetCurrentUserV2();
        if (!user.has_value()) return "";
        auto& u = user.value();
        const std::string avatarUrl = u.AvatarUrl(
            discordpp::UserHandle::AvatarType::Png, discordpp::UserHandle::AvatarType::Png);
        std::string json = "{";
        json += "\"id\":\"" + std::to_string(u.Id()) + "\",";
        json += "\"username\":\"" + jsonEscape(u.Username()) + "\",";
        json += "\"displayName\":\"" + jsonEscape(u.DisplayName()) + "\",";
        json += "\"avatarUrl\":\"" + jsonEscape(avatarUrl) + "\"";
        json += "}";
        return json;
    }, "");
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_psplauncher_discord_DiscordNativeBridge_nativeGetFriendsJson(
    JNIEnv* env, jobject /*thiz*/) {
    if (!gClient) return env->NewStringUTF("[]");
    const std::string result = awaitJson([]() -> std::string {
        auto relationships = gClient->GetRelationships();
        std::string json = "[";
        bool first = true;
        for (auto& rel : relationships) {
            if (rel.DiscordRelationshipType() != discordpp::RelationshipType::Friend &&
                rel.GameRelationshipType() != discordpp::RelationshipType::Friend) {
                continue;
            }
            auto user = rel.User();
            if (!user.has_value()) continue;
            auto& u = user.value();
            const std::string avatarUrl = u.AvatarUrl(
                discordpp::UserHandle::AvatarType::Png, discordpp::UserHandle::AvatarType::Png);
            std::string actName, actDetails, actState;
            auto activity = u.GameActivity();
            if (activity.has_value()) {
                actName = activity->Name();
                if (activity->Details().has_value()) actDetails = activity->Details().value();
                if (activity->State().has_value()) actState = activity->State().value();
            }
            if (!first) json += ",";
            first = false;
            json += "{";
            json += "\"id\":\"" + std::to_string(u.Id()) + "\",";
            json += "\"username\":\"" + jsonEscape(u.Username()) + "\",";
            json += "\"displayName\":\"" + jsonEscape(u.DisplayName()) + "\",";
            json += "\"avatarUrl\":\"" + jsonEscape(avatarUrl) + "\",";
            json += "\"activityName\":\"" + jsonEscape(actName) + "\",";
            json += "\"activityDetails\":\"" + jsonEscape(actDetails) + "\",";
            json += "\"activityState\":\"" + jsonEscape(actState) + "\",";
            json += "\"status\":" + std::to_string(static_cast<int>(u.Status()));
            json += "}";
        }
        json += "]";
        return json;
    }, "[]");
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_psplauncher_discord_DiscordNativeBridge_nativeSetActivity(
    JNIEnv* env, jobject /*thiz*/, jstring jName, jstring jDetails) {
    if (!gClient) return;
    const std::string name = jstr(env, jName);
    const std::string details = jstr(env, jDetails);
    post([name, details]() {
        discordpp::Activity activity{};
        activity.SetType(discordpp::ActivityTypes::Playing);
        activity.SetName(name);
        if (!details.empty()) activity.SetDetails(details);
        gClient->UpdateRichPresence(std::move(activity), [](discordpp::ClientResult /*r*/) {});
    });
}

JNIEXPORT void JNICALL
Java_com_psplauncher_discord_DiscordNativeBridge_nativeClearActivity(
    JNIEnv* /*env*/, jobject /*thiz*/) {
    if (!gClient) return;
    post([]() { gClient->ClearRichPresence(); });
}

JNIEXPORT void JNICALL
Java_com_psplauncher_discord_DiscordNativeBridge_nativeDisconnect(
    JNIEnv* /*env*/, jobject /*thiz*/) {
    if (!gClient) return;
    auto done = std::make_shared<std::promise<void>>();
    auto future = done->get_future();
    post([done]() {
        gClient->Disconnect();
        done->set_value();
    });
    future.wait_for(std::chrono::seconds(5));
}

}  // extern "C"
